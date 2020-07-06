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
import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorage
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class WalletRestoration(
    private val storage: BackupStorage,
    private val workingDir: File,
    private val encryptionAlgorithm: SymmetricEncryptionAlgorithm,
    private val compressionMethod: CompressionMethod
) {

    fun run(password: CharArray) {
        require(storage.backupExists()) { "Backup does not exist" }
        val time = System.currentTimeMillis()
        val encryptedArchive = File(workingDir, "restore_bundle_$time")
        val archive = File(workingDir, "restore_archive_$time.${compressionMethod.extension}")
        val restoreDir = File(workingDir, "restore/").apply { mkdir() }
        try {
            storage.downloadBackup(encryptedArchive)
            encryptionAlgorithm.decrypt(
                password,
                { FileInputStream(encryptedArchive) },
                { FileOutputStream(archive) })
            encryptedArchive.delete()
            compressionMethod.unpack(archive, restoreDir)
            archive.delete()
            BACKUP_FILES.forEach {
                File(restoreDir, it).apply { copyTo(File(workingDir, name), overwrite = true) }
            }
            restoreDir.deleteRecursively()
        } catch (e: Exception) {
            Logger.e(e, "Exception occurred during restoration: $e")
            encryptedArchive.delete()
            archive.delete()
            restoreDir.deleteRecursively()
            throw e
        }
    }

}

class WalletRestorationFactory(
    private val workingDir: File,
    private val encryptionAlgorithm: SymmetricEncryptionAlgorithm,
    private val compressionMethod: CompressionMethod
) {
    fun create(storage: BackupStorage) =
        WalletRestoration(storage, workingDir, encryptionAlgorithm, compressionMethod)

    companion object {
        fun defaultStrategy(workingDirectory: File) = WalletRestorationFactory(
            workingDirectory,
            SymmetricEncryptionAlgorithm.aes(),
            CompressionMethod.zip()
        )
    }

}
