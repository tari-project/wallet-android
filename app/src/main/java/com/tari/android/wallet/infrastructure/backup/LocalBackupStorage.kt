package com.tari.android.wallet.infrastructure.backup

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.orhanobut.logger.Logger
import com.tari.android.wallet.extension.getLastPathComponent
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import java.io.File

internal class LocalBackupStorage(
    private val context: Context,
    private val sharedPrefs: SharedPrefsWrapper,
    private val walletTempDirPath: String,
    private val backupFileProcessor: BackupFileProcessor
) : BackupStorage {

    override fun setup(hostFragment: Fragment) {
        hostFragment.startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
            REQUEST_CODE_PICK_BACKUP_FOLDER
        )
    }

    override suspend fun onSetupActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        if (requestCode != REQUEST_CODE_PICK_BACKUP_FOLDER) return
        when (resultCode) {
            Activity.RESULT_OK -> {
                val uri = intent?.data
                if (uri != null) {
                    Logger.d("Backup URI selected: $uri")
                    sharedPrefs.localBackupFolderURI = uri
                    // persist permissions
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } else {
                    throw BackupStorageSetupException("No folder was selected.")
                }
            }
            Activity.RESULT_CANCELED -> {
                throw BackupStorageSetupCancelled()
            }
        }
    }

    override suspend fun backup(newPassword: CharArray?): DateTime {
        val backupFolderURI = sharedPrefs.localBackupFolderURI
            ?: throw BackupInterruptedException("Local backup folder URI is not set.")
        val backupFolder = DocumentFile.fromTreeUri(context, backupFolderURI)
            ?: throw BackupInterruptedException("Folder could not be accessed.")
        Logger.d("Backup to folder $backupFolderURI.")

        return withContext(Dispatchers.IO) {
            val (backupFile, backupDate, mimeType) = backupFileProcessor.generateBackupFile(newPassword)
            val backupFileName = backupFile.getLastPathComponent()!!
            // create target file
            val targetBackupFile = backupFolder.createFile(
                mimeType,
                backupFileName
            ) ?: throw BackupInterruptedException("Target backup file could not be created.")
            // write to target file
            context.contentResolver.openOutputStream(targetBackupFile.uri).use { outputStream ->
                FileUtils.copyFile(backupFile, outputStream)
            }
            // update backup password
            if (newPassword != null) {
                sharedPrefs.backupPassword = newPassword
            }
            try {
                // clear temp folder
                backupFileProcessor.clearTempFolder()
                // delete older backup files
                for (existingFile in backupFolder.listFiles()) {
                    // delete if it's an old backup file
                    if (existingFile.isFile
                        && BackupNamingPolicy.isBackupFileName(existingFile.name ?: "")
                        && existingFile.name != backupFileName
                    ) {
                        existingFile.delete()
                    }
                }
            } catch (e: Exception) {
                Logger.e(
                    e,
                    "Ignorable backup error while clearing temporary and old files."
                )
            }
            return@withContext backupDate
        }
    }

    override suspend fun deleteAllBackupFiles() {
        val backupFolderURI = sharedPrefs.localBackupFolderURI
            ?: throw BackupInterruptedException("Local backup folder URI is not set.")
        val backupFolder = DocumentFile.fromTreeUri(context, backupFolderURI)
            ?: throw BackupInterruptedException("Folder could not be accessed.")
        // delete all backup files
        for (existingFile in backupFolder.listFiles()) {
            // delete if it's an old backup file
            if (existingFile.isFile
                && BackupNamingPolicy.isBackupFileName(existingFile.name ?: "")
            ) {
                existingFile.delete()
            }
        }
    }

    override suspend fun signOut() {
        sharedPrefs.localBackupFolderURI = null
        backupFileProcessor.clearTempFolder()
    }

    override suspend fun restoreLatestBackup(password: String?) {
        val backupFolderURI = sharedPrefs.localBackupFolderURI
            ?: throw BackupStorageTamperedException("Backup storage not accessible.")
        val backupFolder = DocumentFile.fromTreeUri(context, backupFolderURI)
            ?: throw BackupStorageTamperedException("Backup storage is not a folder.")
        val backupFiles = backupFolder.listFiles().filter { file ->
            BackupNamingPolicy.getDateFromBackupFileName(file.name ?: "") != null
        }
        if (backupFiles.isEmpty()) {
            throw BackupStorageTamperedException("Backup file not found in folder.")
        }
        backupFiles.sortedBy { it.name!! }
        withContext(Dispatchers.IO) {
            // copy file to temp location
            val tempFile = File(walletTempDirPath, backupFiles.last().name!!)
            // create file & fetch if it hasn't been fetched before
            if (!tempFile.exists()) {
                tempFile.createNewFile()
                context.contentResolver.openInputStream(backupFiles.last().uri).use { inputStream ->
                    FileUtils.copyInputStreamToFile(inputStream, tempFile)
                }
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
        }
    }

    override suspend fun hasBackupForDate(backupDate: DateTime): Boolean {
        val backupFolderURI = sharedPrefs.localBackupFolderURI
            ?: return false
        val backupFolder = DocumentFile.fromTreeUri(context, backupFolderURI)
            ?: return false
        val expectedBackupFileName = BackupNamingPolicy.getBackupFileName(backupDate)
        return backupFolder.listFiles().firstOrNull {
            it.isFile && it.name?.contains(expectedBackupFileName) == true
        } != null
    }

    companion object {
        private const val REQUEST_CODE_PICK_BACKUP_FOLDER = 1354
    }

}