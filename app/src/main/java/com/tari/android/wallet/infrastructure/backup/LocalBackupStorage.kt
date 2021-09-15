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
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.getLastPathComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import java.io.File

/**
 * Local external file storage.
 *
 * @author The Tari Development Team
 */
// todo review
internal class LocalBackupStorage(
    private val context: Context,
    private val sharedPrefs: SharedPrefsRepository,
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
            ) ?: throw BackupStorageAuthRevokedException() // file could not be created
            // write to target file
            context.contentResolver.openOutputStream(targetBackupFile.uri).use { outputStream ->
                FileUtils.copyFile(backupFile, outputStream)
            }
            // update backup password
            if (newPassword != null) {
                sharedPrefs.backupPassword = newPassword.toString()
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
            if (tempFile.parentFile?.exists() == false) {
                tempFile.parentFile?.mkdirs()
            }
            if (!tempFile.exists()) {
                tempFile.createNewFile()
                context.contentResolver.openInputStream(backupFiles.last().uri).use { inputStream ->
                    FileUtils.copyInputStreamToFile(inputStream, tempFile)
                }
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
            // restore successful, turn on automated backup
            sharedPrefs.localBackupFolderURI = backupFolderURI
            sharedPrefs.lastSuccessfulBackupDate = BackupNamingPolicy.getDateFromBackupFileName(tempFile.name)
            sharedPrefs.backupPassword = password
        }
    }

    override suspend fun hasBackupForDate(date: DateTime): Boolean {
        val backupFolderURI = sharedPrefs.localBackupFolderURI
            ?: return false
        val backupFolder = DocumentFile.fromTreeUri(context, backupFolderURI)
            ?: throw BackupStorageAuthRevokedException()
        if (!backupFolder.exists()) {
            throw BackupStorageAuthRevokedException()
        }
        val expectedBackupFileName = BackupNamingPolicy.getBackupFileName(date)
        return backupFolder.listFiles().firstOrNull {
            it.isFile && it.name?.contains(expectedBackupFileName) == true
        } != null
    }

    companion object {
        private const val REQUEST_CODE_PICK_BACKUP_FOLDER = 1354
    }
}