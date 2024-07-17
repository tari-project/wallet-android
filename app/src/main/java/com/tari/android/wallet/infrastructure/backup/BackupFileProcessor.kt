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

import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.extension.encrypt
import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.DateTime
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupFileProcessor @Inject constructor(
    private val backupSettingsRepository: BackupPrefRepository,
    private val securityPrefRepository: SecurityPrefRepository,
    private val walletConfig: WalletConfig,
    private val namingPolicy: BackupNamingPolicy,
) {
    private val logger
        get() = Logger.t(BackupFileProcessor::class.simpleName)

    private val mutex = Mutex()

    suspend fun generateBackupFile(): Triple<File, DateTime, String> = mutex.withLock {
        // create partial backup in temp folder if password not set

        delay(1000)

        val databaseFile = File(walletConfig.walletDatabaseFilePath)
        val backupPassword = backupSettingsRepository.backupPassword
        val backupFileName = namingPolicy.getBackupFileName(backupPassword.isNullOrEmpty())
        val outputFile = File(walletConfig.getWalletTempDirPath(), backupFileName)
        val backupDate = DateTime.now()

        if (backupPassword.isNullOrEmpty()) {
            val mimeType = "application/json"

            val ffiWallet = FFIWallet.instance!!
            val outputs = ffiWallet.getUnbindedOutputs(FFIError())
            val hexString = HexString(ffiWallet.getWalletAddress().getByteVector()) // TODO don't hex, I think
            val jsonObject = BackupUtxos(outputs.map { it.json }, hexString.hex)
            val json = Gson().toJson(jsonObject)

            outputFile.bufferedWriter().use { it.append(json) }

            logger.i("Partial files was generated")
            return Triple(outputFile, backupDate, mimeType)
        } else {
            val passphrase = securityPrefRepository.databasePassphrase!!
            val passphraseFile = File(walletConfig.getWalletTempDirPath(), namingPolicy.getPassphraseFileName())
            passphraseFile.bufferedWriter().use { it.append(passphrase) }

            val encryptionAlgorithm = SymmetricEncryptionAlgorithm.aes()

            val compressionMethod = CompressionMethod.zip()
            var filesToBackup = compressionMethod.compress(outputFile.absolutePath, listOf(databaseFile, passphraseFile))
            // encrypt the file if password is set
            val copiedFile = filesToBackup.copyTo(File(filesToBackup.absolutePath + "_temp"))
            val outputFilePath = File(walletConfig.getWalletTempDirPath(), backupFileName).absolutePath
            filesToBackup = copiedFile.encrypt(encryptionAlgorithm, backupPassword.toCharArray(), outputFilePath)
            copiedFile.delete()

            logger.i("Full backup files was generated")
            return Triple(filesToBackup, backupDate, encryptionAlgorithm.mimeType)
        }
    }

    fun restoreBackupFile(file: File, password: String? = null) {

        if (password.isNullOrEmpty() && !file.absolutePath.endsWith(".json")) {
            throw BackupFileIsEncryptedException("Restored file is encrypted, password is needed")
        }

        if (password.isNullOrEmpty()) {
            val json = file.readText()
            val jsonObject = Gson().fromJson(json, BackupUtxos::class.java)
            backupSettingsRepository.restoredTxs = jsonObject
        } else {
            val walletFilesDir = File(walletConfig.getWalletFilesDirPath())
            val compressionMethod = CompressionMethod.zip()

            val tempFileName = "temp_" + System.currentTimeMillis() + "." + compressionMethod.extension
            val unencryptedCompressedFile = File(walletConfig.getWalletTempDirPath(), tempFileName)

            unencryptedCompressedFile.createNewFile()

            SymmetricEncryptionAlgorithm.aes().decrypt(password.toCharArray(), { file.inputStream() }, { unencryptedCompressedFile.outputStream() })

            CompressionMethod.zip().uncompress(unencryptedCompressedFile, walletFilesDir)

            val passphraseFile = File(walletConfig.getWalletFilesDirPath(), namingPolicy.getPassphraseFileName())
            val passphrase = passphraseFile.readText()

            securityPrefRepository.databasePassphrase = passphrase

            unencryptedCompressedFile.delete()

            if (!File(walletConfig.walletDatabaseFilePath).exists()) {
                // delete uncompressed files
                walletFilesDir.deleteRecursively()
                throw BackupStorageTamperedException("Invalid encrypted backup.")
            }
            logger.i("Backup file was restored")
        }
    }

    fun clearTempFolder() {
        try {
            File(walletConfig.getWalletTempDirPath()).listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            Logger.i(e.toString() + "Ignorable backup error while clearing temporary and old files.")
        }
    }
}

