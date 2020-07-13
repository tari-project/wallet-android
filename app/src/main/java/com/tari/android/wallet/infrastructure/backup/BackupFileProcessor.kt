package com.tari.android.wallet.infrastructure.backup

import com.orhanobut.logger.Logger
import com.tari.android.wallet.extension.compress
import com.tari.android.wallet.extension.encrypt
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import org.joda.time.DateTime
import java.io.File

internal class BackupFileProcessor(
    private val sharedPrefs: SharedPrefsWrapper,
    private val walletFilesDirPath: String,
    private val walletDatabaseFilePath: String,
    private val walletTempDirPath: String
) {

    fun generateBackupFile(newPassword: CharArray? = null): Triple<File, DateTime, String> {
        // create partial backup in temp folder if password not set
        var databaseFile = File(walletDatabaseFilePath)
        val backupPassword = newPassword ?: sharedPrefs.backupPassword
        val backupDate = DateTime.now()
        if (backupPassword == null) {
            val wallet = FFIWallet.instance
                ?: throw BackupInterruptedException("Wallet instance not ready.")
            databaseFile = File(walletTempDirPath, Constants.Wallet.walletDBFullFileName)
            wallet.doPartialBackup(databaseFile.absolutePath)
            if (!databaseFile.exists()) {
                throw BackupInterruptedException("Partial backup file could not be created.")
            }
        }
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