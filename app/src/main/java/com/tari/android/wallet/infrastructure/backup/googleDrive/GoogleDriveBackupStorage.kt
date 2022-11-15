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
package com.tari.android.wallet.infrastructure.backup.googleDrive

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
import com.tari.android.wallet.extension.getLastPathComponent
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

class GoogleDriveBackupStorage(
    private val context: Context,
    private val namingPolicy: BackupNamingPolicy,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val walletTempDirPath: String,
    private val backupFileProcessor: BackupFileProcessor
) : BackupStorage {

    private val logger
        get() = Logger.t(GoogleDriveBackupStorage::class.simpleName)

    private val googleClient: GoogleSignInClient = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
    )

    private var drive: Drive? = null

    init {
        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (googleAccount != null) {
            val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
                .apply { selectedAccount = googleAccount.account }
            drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
                .setApplicationName(context.resources.getString(R.string.app_name))
                .build()
        }
    }

    override fun setup(hostFragment: Fragment) {
        hostFragment.startActivityForResult(googleClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> when (resultCode) {
                Activity.RESULT_OK -> drive = getDrive(intent)
                Activity.RESULT_CANCELED -> throw BackupStorageSetupCancelled()
                else -> throw BackupStorageSetupException("Google Drive setup error.")
            }
        }
        return true
    }

    private suspend fun getDrive(intent: Intent?): Drive {
        return suspendCoroutine { continuation ->
            GoogleSignIn.getSignedInAccountFromIntent(intent)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                    val credential = GoogleAccountCredential.usingOAuth2(context, listOf(DriveScopes.DRIVE_APPDATA))
                        .apply { selectedAccount = googleAccount.account }
                    drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
                        .setApplicationName(context.resources.getString(R.string.app_name))
                        .build()
                    continuation.resumeWith(Result.success(drive!!))
                }
                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                .addOnCanceledListener { continuation.resumeWith(Result.failure(BackupInterruptedException(""))) }
        }
    }

    override suspend fun backup(): DateTime {
        return withContext(Dispatchers.IO) {
            val (backupFile, backupDate, mimeType) = backupFileProcessor.generateBackupFile()
            // upload file
            try {
                createBackupFile(backupFile, mimeType)
            } catch (exception: UserRecoverableAuthIOException) {
                throw BackupStorageAuthRevokedException()
            } catch (exception: GoogleJsonResponseException) {
                for (error in exception.details.errors) {
                    if (error.reason == "storageQuotaExceeded") {
                        throw BackupStorageFullException()
                    }
                }
                throw exception
            } catch (exception: Exception) {
                throw exception
            }
            try {
                backupFileProcessor.clearTempFolder()
            } catch (e: Exception) {
                Logger.e(e, "Ignorable backup error while clearing temporary and old files.")
            }
            return@withContext backupDate
        }
    }

    private fun createBackupFile(file: File, mimeType: String) {
        val metadata: com.google.api.services.drive.model.File =
            com.google.api.services.drive.model.File()
                .setParents(listOf(DRIVE_BACKUP_PARENT_FOLDER_NAME))
                .setMimeType(mimeType)
                .setName(file.getLastPathComponent())
        drive!!.files()
            .create(metadata, FileContent(mimeType, file))
            .setFields("id")
            .execute()
            ?: throw IOException("Null result when requesting file creation.")
    }

    override suspend fun hasBackup(): Boolean {
        try {
            val latestBackupFileName = getLastBackupFileIdAndName()?.second ?: return false
            return latestBackupFileName.contains(namingPolicy.getBackupFileName())
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
            val tempFile = File(tempFolder, backupFileName)
            if (tempFolder.parentFile?.exists() != true) {
                tempFolder.parentFile?.mkdir()
            }
            if (!tempFolder.exists()) {
                tempFolder.mkdir()
            }
            if (!tempFile.exists()) {
                tempFile.createNewFile()
            }
            FileOutputStream(tempFile).use { targetOutputStream ->
                drive!!.files().get(backupFileId).executeMediaAndDownloadTo(targetOutputStream)
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
        }
    }

    private fun getLastBackupFileIdAndName(): Pair<String, String>? {
        val file = searchForBackups().files.firstOrNull { namingPolicy.isBackupFileName(it.name) } ?: return null
        return file.id to file.name
    }

    private fun searchForBackups(pageToken: String? = null): FileList =
        drive!!.files().list()
            .setSpaces(DRIVE_BACKUP_PARENT_FOLDER_NAME)
            .setQ("'$DRIVE_BACKUP_PARENT_FOLDER_NAME' in parents")
            .setFields("nextPageToken, files(id, name)")
            .setPageToken(pageToken)
            .execute()

    override suspend fun deleteAllBackupFiles() {
        val driveFiles = drive?.files() ?: return
        var pageToken: String? = null
        do {
            pageToken = searchForBackups(pageToken).let {
                it.files.forEach { file ->
                    if (file.name == namingPolicy.getBackupFileName()) {
                        driveFiles.delete(file.id).execute()
                    }
                }
                it.nextPageToken
            }
        } while (pageToken != null)
    }

    override suspend fun signOut() {
        suspendCoroutine { continuation ->
            try {
                backupFileProcessor.clearTempFolder()
                googleClient.signOut()
                    .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                    .addOnCompleteListener { continuation.resumeWith(Result.success(Unit)) }
            } catch (e: Throwable) {
                logger.e(e, "Sentry failed with already resumed for no reason")
            }
        }
    }

    private companion object {
        private const val DRIVE_BACKUP_PARENT_FOLDER_NAME = "appDataFolder"
        private const val REQUEST_CODE_SIGN_IN = 1356
    }

}
