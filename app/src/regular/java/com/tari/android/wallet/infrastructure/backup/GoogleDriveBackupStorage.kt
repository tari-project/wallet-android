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
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.getLastPathComponent
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal class GoogleDriveBackupStorage(
    private val context: Context,
    private val sharedPrefs: SharedPrefsWrapper,
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

    override fun setup(hostFragment: Fragment) {
        hostFragment.startActivityForResult(googleClient.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override suspend fun onSetupActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        drive = getDrive(intent)
                    }
                    Activity.RESULT_CANCELED -> {
                        throw BackupStorageSetupCancelled()
                    }
                    else -> {
                        throw BackupStorageSetupException("Google Drive setup error.")
                    }
                }
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
            } catch (exception: Exception) {
                throw exception
            }
            // update backup password
            if (newPassword != null) {
                sharedPrefs.backupPassword = newPassword
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
            // copy file to temp location
            val tempFile = File(walletTempDirPath, backupFileName)
            // create file & fetch if it hasn't been fetched before
            if (!tempFile.exists()) {
                tempFile.createNewFile()
                FileOutputStream(tempFile).use { targetOutputStream ->
                    drive.files().get(backupFileId).executeMediaAndDownloadTo(targetOutputStream)
                }
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
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
        val latestBackupFile = backups.maxBy { it.first }?.second
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