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
package com.tari.android.wallet.infrastructure.security.encryption

import java.io.InputStream
import java.io.OutputStream
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec

interface SymmetricEncryptionAlgorithm {

    fun encrypt(
        key: CharArray,
        inputStreamProvider: () -> InputStream,
        outputStreamProvider: () -> OutputStream
    )

    companion object {
        fun aes(): SymmetricEncryptionAlgorithm = AES()
    }

}

private class AES(
    private val digest: MessageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM)
) : SymmetricEncryptionAlgorithm {

    // TODO add salt?
    // TODO add padding and initialization vector?
    override fun encrypt(
        key: CharArray,
        inputStreamProvider: () -> InputStream,
        outputStreamProvider: () -> OutputStream
    ) {
        val input = inputStreamProvider()
        val output = outputStreamProvider()
        val encryptionKey: ByteArray = key.toString().toByteArray(Charsets.UTF_8)
            .let { digest.digest(it).copyOf(KEY_SIZE_128_BIT) }
        val sks = SecretKeySpec(encryptionKey, ALGORITHM_AES)
        val cipher =
            Cipher.getInstance(CIPHER_AES_TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, sks) }
        val cos = CipherOutputStream(output, cipher)
        val buffer = ByteArray(8)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            cos.write(buffer, 0, bytesRead)
        }
        cos.flush()
        cos.close()
        input.close()
    }

    private companion object {
        private const val DIGEST_ALGORITHM = "SHA-1"
        private const val ALGORITHM_AES = "AES"
        private const val CIPHER_AES_TRANSFORMATION = ALGORITHM_AES
        private const val KEY_SIZE_128_BIT = 16
    }

}
