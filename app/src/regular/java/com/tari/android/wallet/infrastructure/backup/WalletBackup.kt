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

import com.tari.android.wallet.infrastructure.backup.compress.CompressionMethod
import com.tari.android.wallet.infrastructure.security.encryption.SymmetricEncryptionAlgorithm
import com.tari.android.wallet.util.Constants
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


interface WalletBackup {
    fun run(key: CharArray): File

    companion object {
        fun defaultStrategy(workingDir: File): WalletBackup = zipOnly(workingDir)

        fun aesEncryptedZip(workingDir: File): WalletBackup = CompositeWalletBackup(
            workingDir,
            SymmetricEncryptionAlgorithm.aes(),
            CompressionMethod.zip()
        )

        fun zipOnly(workingDir: File): WalletBackup =
            CompressionOnlyBackup(workingDir, CompressionMethod.zip())

    }
}

val BACKUP_FILES = arrayOf(Constants.Wallet.walletDBFullFileName)

private class CompositeWalletBackup(
    private val workingDir: File,
    private val encryptionAlgorithm: SymmetricEncryptionAlgorithm,
    private val compressionMethod: CompressionMethod
) : WalletBackup {

    override fun run(key: CharArray): File {
        val backupTime = System.currentTimeMillis()
        val archive = compressionMethod.compress(
            "${workingDir.absolutePath}/temp_$backupTime.${compressionMethod.extension}",
            BACKUP_FILES.map { File(workingDir, it) })
        val encryptedArchiveFile = File(workingDir, "backup_$backupTime")
        encryptionAlgorithm.encrypt(
            key, { FileInputStream(archive) }, { FileOutputStream(encryptedArchiveFile) }
        )
        archive.delete()
        return encryptedArchiveFile
    }

}

private class CompressionOnlyBackup(
    private val workingDir: File,
    private val compressionMethod: CompressionMethod
) : WalletBackup {
    override fun run(key: CharArray): File = compressionMethod.compress(
        "${workingDir.absolutePath}/backup_${System.currentTimeMillis()}.${compressionMethod.extension}",
        BACKUP_FILES.map { File(workingDir, it) })
}
