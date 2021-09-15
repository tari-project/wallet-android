/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.infrastructure.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.getLastPathComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal class GoogleDriveBackupStorage(
    private val context: Context,
    private val sharedPrefs: SharedPrefsRepository,
    private val walletTempDirPath: String,
    private val backupFileProcessor: BackupFileProcessor
) : BackupStorage {

    private val googleClient: GoogleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    )

    private lateinit var drive: Drive

    init {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {
            val credential =
                GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE_APPDATA)
                ).apply { selectedAccount = googleAccount.account }
            drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
                .setApplicationName(context.resources.getString(R.string.app_name))
                .build()
        }
    }

    override fun setup(hostFragment: Fragment) {
        hostFragment.startActivityForResult(googleClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override suspend fun onSetupActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> when (resultCode) {
                Activity.RESULT_OK -> drive = getDrive(intent)
                Activity.RESULT_CANCELED -> throw BackupStorageSetupCancelled()
                else -> throw BackupStorageSetupException("Google Drive setup error.")
            }
        }
    }

    private suspend fun getDrive(intent: Intent?): Drive {
        return suspendCoroutine { continuation ->
            GoogleSignIn.getSignedInAccountFromIntent(intent)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                    val credential = GoogleAccountCredential.usingOAuth2(
                        context,
                        listOf(DriveScopes.DRIVE_APPDATA)
                    ).apply { selectedAccount = googleAccount.account }
                    val drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
                        .setApplicationName(context.resources.getString(R.string.app_name))
                        .build()
                    continuation.resumeWith(Result.success(drive))
                }
                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                .addOnCanceledListener {
                    continuation.resumeWith(
                        Result.failure(BackupInterruptedException(""))
                    )
                }
        }
    }

    override suspend fun backup(newPassword: CharArray?): DateTime {
        return withContext(Dispatchers.IO) {
            val (backupFile, backupDate, mimeType) = backupFileProcessor.generateBackupFile(
                newPassword
            )
            // upload file
            try {
                createBackupFile(backupFile, mimeType)
            } catch (exception: UserRecoverableAuthIOException) {
                throw BackupStorageAuthRevokedException()
            } catch(exception: GoogleJsonResponseException) {
                for (error in exception.details.errors) {
                    if (error.reason == "storageQuotaExceeded") {
                        throw BackupStorageFullException()
                    }
                }
                throw exception
            } catch (exception: Exception) {
                throw exception
            }
            // update backup password
            if (newPassword != null) {
                sharedPrefs.backupPassword = newPassword.toString()
            }
            try {
                backupFileProcessor.clearTempFolder()
                // delete older backups
                deleteAllBackupFiles(excludeBackupWithDate = backupDate)
            } catch (e: Exception) {
                Logger.e(
                    e,
                    "Ignorable backup error while clearing temporary and old files."
                )
            }
            return@withContext backupDate
        }
    }

    private fun createBackupFile(
        file: File,
        mimeType: String
    ) {
        val metadata: com.google.api.services.drive.model.File =
            com.google.api.services.drive.model.File()
                .setParents(listOf(DRIVE_BACKUP_PARENT_FOLDER_NAME))
                .setMimeType(mimeType)
                .setName(file.getLastPathComponent()!!)
        drive.files()
            .create(metadata, FileContent(mimeType, file))
            .setFields("id")
            .execute()
            ?: throw IOException("Null result when requesting file creation.")
    }

    override suspend fun hasBackupForDate(date: DateTime): Boolean {
        try {
            val latestBackupFileName = getLastBackupFileIdAndName()?.second ?: return false
            return latestBackupFileName.contains(BackupNamingPolicy.getBackupFileName(date))
        } catch (exception: UserRecoverableAuthIOException) {
            throw BackupStorageAuthRevokedException()
        } catch (exception: Exception) {
            throw exception
        }
    }

    override suspend fun restoreLatestBackup(password: String?) {
        val (backupFileId, backupFileName) = try {
            getLastBackupFileIdAndName()
        } catch (e: UserRecoverableAuthIOException) {
            throw BackupStorageAuthRevokedException()
        } catch (e: Exception) {
            throw e
        } ?: throw BackupStorageTamperedException("Backup file not found in folder.")
        withContext(Dispatchers.IO) {
            val tempFolder = File(walletTempDirPath)
            if (!tempFolder.exists()) tempFolder.mkdir()
            // copy file to temp location
            val tempFile = File(tempFolder, backupFileName)
            // create file & fetch if it hasn't been fetched before
            if (!tempFile.exists()) {
                tempFile.createNewFile()
                FileOutputStream(tempFile).use { targetOutputStream ->
                    drive.files().get(backupFileId).executeMediaAndDownloadTo(targetOutputStream)
                }
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
            // restore successful, turn on automated backup
            sharedPrefs.lastSuccessfulBackupDate = BackupNamingPolicy.getDateFromBackupFileName(tempFile.name)
            sharedPrefs.backupPassword = password
        }
    }

    private fun getLastBackupFileIdAndName(): Pair<String, String>? {
        val backups = mutableListOf<Pair<DateTime, com.google.api.services.drive.model.File>>()
        var pageToken: String? = null
        do {
            val result: FileList = searchForBackups(pageToken)
            result.files.forEach {
                BackupNamingPolicy.getDateFromBackupFileName(it.name)
                    ?.let { time -> backups.add(time to it) }
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
        val latestBackupFile = backups.maxByOrNull { it.first }?.second
        return if (latestBackupFile != null) {
            (latestBackupFile.id to latestBackupFile.name)
        } else {
            null
        }
    }

    private fun searchForBackups(pageToken: String?): FileList =
        drive.files().list()
            .setSpaces(DRIVE_BACKUP_PARENT_FOLDER_NAME)
            .setQ("'${DRIVE_BACKUP_PARENT_FOLDER_NAME}' in parents")
            .setFields("nextPageToken, files(id, name)")
            .setPageToken(pageToken)
            .execute()

    override suspend fun deleteAllBackupFiles() {
        deleteAllBackupFiles(excludeBackupWithDate = null)
    }

    private fun deleteAllBackupFiles(excludeBackupWithDate: DateTime?) {
        val driveFiles = drive.files()
        var pageToken: String? = null
        do {
            val result: FileList = searchForBackups(pageToken)
            result.files.forEach { file ->
                val excludeName: String? = if (excludeBackupWithDate != null) {
                    BackupNamingPolicy.getBackupFileName(excludeBackupWithDate)
                } else {
                    null
                }
                if (excludeName == null || !file.name.contains(excludeName)) {
                    BackupNamingPolicy.getDateFromBackupFileName(file.name)?.let {
                        driveFiles.delete(file.id).execute()
                    }
                }
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
    }

    override suspend fun signOut() {
        suspendCoroutine<Unit> { continuation ->
            backupFileProcessor.clearTempFolder()
            googleClient.signOut()
                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                .addOnSuccessListener { continuation.resumeWith(Result.success(Unit)) }
        }
    }

    private companion object {
        private const val DRIVE_BACKUP_PARENT_FOLDER_NAME = "appDataFolder"
        private const val REQUEST_CODE_SIGN_IN = 1356
    }

}
