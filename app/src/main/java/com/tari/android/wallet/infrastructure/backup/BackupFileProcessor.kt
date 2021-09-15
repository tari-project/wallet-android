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

import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.compress
import com.tari.android.wallet.extension.encrypt
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import org.joda.time.DateTime
import java.io.File

/**
 * Utility functions to prepares the backup or restoration files.
 *
 * @author The Tari Development Team
 */
internal class BackupFileProcessor(
    private val sharedPrefs: SharedPrefsRepository,
    private val walletFilesDirPath: String,
    private val walletDatabaseFilePath: String,
    private val walletTempDirPath: String
) {

    fun generateBackupFile(newPassword: CharArray? = null): Triple<File, DateTime, String> {
        // decrypt database
        FFIWallet.instance?.removeEncryption()

        // create partial backup in temp folder if password not set
        val databaseFile = File(walletDatabaseFilePath)
        val backupPassword = newPassword ?: sharedPrefs.backupPassword?.toCharArray()
        val backupDate = DateTime.now()
        // zip the file
        val compressionMethod = CompressionMethod.zip()
        var mimeType = compressionMethod.mimeType
        val backupFileName = BackupNamingPolicy.getBackupFileName(backupDate)
        val compressedFile = File(
            walletTempDirPath,
            "$backupFileName.${compressionMethod.extension}"
        )
        var fileToBackup = listOf(databaseFile).compress(
            CompressionMethod.zip(),
            compressedFile.absolutePath
        )
        // encrypt the file if password is set
        val encryptionAlgorithm = SymmetricEncryptionAlgorithm.aes()
        if (backupPassword != null) {
            val targetFileName = "$backupFileName.${encryptionAlgorithm.extension}"
            fileToBackup = fileToBackup.encrypt(
                encryptionAlgorithm,
                backupPassword,
                File(walletTempDirPath, targetFileName).absolutePath
            )
            mimeType = encryptionAlgorithm.mimeType
        }
        // encrypt after finish backup
        FFIWallet.instance?.enableEncryption()

        return Triple(fileToBackup, backupDate, mimeType)
    }

    fun restoreBackupFile(file: File, password: String? = null) {
        val walletFilesDir = File(walletFilesDirPath)
        val compressionMethod = CompressionMethod.zip()
        // encrypt file if password is supplied
        if (!password.isNullOrEmpty()) {
            val unencryptedCompressedFile = File(
                walletTempDirPath,
                "temp_" + System.currentTimeMillis() + "." + compressionMethod.extension
            )
            unencryptedCompressedFile.createNewFile()
            SymmetricEncryptionAlgorithm.aes().decrypt(
                password.toCharArray(),
                { file.inputStream() },
                { unencryptedCompressedFile.outputStream() }
            )
            CompressionMethod.zip().uncompress(
                unencryptedCompressedFile,
                walletFilesDir
            )
            if (!File(walletDatabaseFilePath).exists()) {
                // delete uncompressed files
                walletFilesDir.deleteRecursively()
                throw BackupStorageTamperedException("Invalid encrypted backup.")
            }
        } else {
            CompressionMethod.zip().uncompress(
                file,
                walletFilesDir
            )
            // check if wallet database file exists
            if (!File(walletDatabaseFilePath).exists()) {
                walletFilesDir.listFiles()?.let { files ->
                    // delete uncompressed files
                    for (extractedFile in files) {
                        if (extractedFile.isFile) { extractedFile.delete() }
                    }
                }
                // throw exception
                throw BackupFileIsEncryptedException(
                    file,
                    "Cannot uncompress. Restored file is encrypted."
                )
            }
        }
    }

    fun clearTempFolder() {
        try {
            File(walletTempDirPath).listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Logger.e(
                e,
                "Ignorable backup error while clearing temporary and old files."
            )
        }
    }

}