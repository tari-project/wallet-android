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
package com.tari.android.wallet.infrastructure.backup.local

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.extension.getLastPathComponent
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import java.io.File

class LocalBackupStorage(
    private val context: Context,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val namingPolicy: BackupNamingPolicy,
    private val walletTempDirPath: String,
    private val networkRepository: NetworkRepository,
    private val backupFileProcessor: BackupFileProcessor
) : BackupStorage {

    private val logger
        get() = Logger.t(LocalBackupStorage::class.simpleName)

    override fun setup(hostFragment: Fragment) {
        hostFragment.startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT_TREE),
            REQUEST_CODE_PICK_BACKUP_FOLDER
        )
    }

    override suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        if (requestCode != REQUEST_CODE_PICK_BACKUP_FOLDER) return false
        when (resultCode) {
            Activity.RESULT_OK -> {
                val uri = intent?.data
                if (uri != null) {
                    logger.i("Backup URI selected: $uri")
                    backupSettingsRepository.localBackupFolderURI = uri
                    // persist permissions
                    val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                } else {
                    throw BackupStorageSetupException("No folder was selected.")
                }
            }
            Activity.RESULT_CANCELED -> {
                throw BackupStorageSetupCancelled()
            }
        }
        return true
    }

    override suspend fun backup(): DateTime {
        val backupFolder = getBackupFolder()

        return withContext(Dispatchers.IO) {
            val (backupFile, backupDate, mimeType) = backupFileProcessor.generateBackupFile()
            val backupFileName = backupFile.getLastPathComponent()
            // create target file
            val targetBackupFile = backupFolder.createFile(mimeType, backupFileName) ?: throw BackupStorageAuthRevokedException()
            // write to target file
            context.contentResolver.openOutputStream(targetBackupFile.uri).use { outputStream -> FileUtils.copyFile(backupFile, outputStream) }
            try {
                // clear temp folder
                backupFileProcessor.clearTempFolder()
                // delete older backup files
                for (existingFile in backupFolder.listFiles()) {
                    // delete if it's an old backup file
                    if (existingFile.isFile
                        && namingPolicy.isBackupFileName(existingFile.name ?: "")
                        && existingFile.name != backupFileName
                    ) {
                        existingFile.delete()
                    }
                }
            } catch (e: Exception) {
                logger.e(e, "Ignorable backup error while clearing temporary and old files.")
            }
            return@withContext backupDate
        }
    }

    override suspend fun deleteAllBackupFiles() {
        val backupFolder = getBackupFolder()
        // delete all backup files
        for (existingFile in backupFolder.listFiles()) {
            // delete if it's an old backup file
            if (existingFile.isFile && namingPolicy.isBackupFileName(existingFile.name ?: "")
            ) {
                existingFile.delete()
            }
        }
    }

    override suspend fun signOut() {
        backupSettingsRepository.localBackupFolderURI = null
        backupFileProcessor.clearTempFolder()
    }

    override suspend fun restoreLatestBackup(password: String?) {
        val backupFolder = getBackupFolder()
        val backupFiles = backupFolder.listFiles().firstOrNull { documentFile: DocumentFile? ->
            namingPolicy.isBackupFileName(documentFile?.name.orEmpty())
        } ?: throw BackupStorageTamperedException("Backup file not found in folder.")
        withContext(Dispatchers.IO) {
            // copy file to temp location
            val tempFolder = File(walletTempDirPath)
            val tempFile = File(tempFolder, backupFiles.name!!)
            if (tempFolder.parentFile?.exists() != true) {
                tempFolder.parentFile?.mkdir()
            }
            if (!tempFolder.exists()) {
                tempFolder.mkdir()
            }
            if (!tempFile.exists()) {
                tempFile.createNewFile()
            }
            context.contentResolver.openInputStream(backupFiles.uri).use { inputStream ->
                FileUtils.copyInputStreamToFile(inputStream, tempFile)
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
        }
    }

    override suspend fun hasBackup(): Boolean {
        val backupFolderURI = backupSettingsRepository.localBackupFolderURI ?: return false
        val backupFolder = DocumentFile.fromTreeUri(context, backupFolderURI) ?: throw BackupStorageAuthRevokedException()
        if (!backupFolder.exists()) {
            throw BackupStorageAuthRevokedException()
        }
        val expectedBackupFileName = namingPolicy.getBackupFileName()
        return backupFolder.listFiles().firstOrNull { it.isFile && it.name?.contains(expectedBackupFileName) == true } != null
    }

    private fun getBackupFolder(): DocumentFile {
        val backupFolderURI = backupSettingsRepository.localBackupFolderURI ?: throw BackupStorageTamperedException("Backup storage not accessible.")
        val networkFolder = networkRepository.currentNetwork!!.network.displayName
        var rootFolder = DocumentFile.fromTreeUri(context, backupFolderURI) ?: throw BackupStorageTamperedException("Backup storage is not a folder.")
        rootFolder = rootFolder.findFile(networkFolder) ?: (rootFolder.createDirectory(networkFolder) ?: rootFolder)
        return rootFolder
    }

    companion object {
        private const val REQUEST_CODE_PICK_BACKUP_FOLDER = 1354
    }
}