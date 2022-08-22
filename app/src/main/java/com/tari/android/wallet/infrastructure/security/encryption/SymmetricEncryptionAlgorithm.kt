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
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

interface SymmetricEncryptionAlgorithm {

    val extension: String
    val mimeType: String

    fun encrypt(
        password: CharArray,
        sourceStreamProvider: () -> InputStream,
        destinationStreamProvider: () -> OutputStream
    )

    fun decrypt(
        password: CharArray,
        sourceStreamProvider: () -> InputStream,
        destinationStreamProvider: () -> OutputStream
    )

    companion object {

        fun aes(): SymmetricEncryptionAlgorithm = AES()
    }

}

private class AES(private val random: SecureRandom = SecureRandom()) :
    SymmetricEncryptionAlgorithm {

    override val extension: String
        get() = "enc"
    override val mimeType: String
        get() = "application/x-binary"

    override fun encrypt(
        password: CharArray,
        sourceStreamProvider: () -> InputStream,
        destinationStreamProvider: () -> OutputStream
    ) {
        val (salt, keySpec) = defineKeySpec(password.copyOf())
        val cbcInitializationVector = ByteArray(IV_SIZE).apply(random::nextBytes)
        val cipher = Cipher.getInstance(CIPHER_AES_TRANSFORMATION)
            .apply { init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(cbcInitializationVector)) }
        createEncryptedFile(
            sourceStreamProvider,
            destinationStreamProvider,
            cipher,
            cbcInitializationVector,
            salt
        )
    }

    private fun defineKeySpec(password: CharArray): Pair<ByteArray, SecretKeySpec> {
        val salt = ByteArray(SALT_SIZE).apply(random::nextBytes)
        val spec = PBEKeySpec(
            if (password.isEmpty()) charArrayOf('_') else password,
            salt,
            KEY_HASHING_TIMES,
            BIT_KEY_SIZE
        )
        val secret: SecretKey =
            SecretKeyFactory.getInstance(SECRET_KEYGEN_ALGORITHM).generateSecret(spec)
        spec.clearPassword()
        Arrays.fill(password, NULL_BYTE.toInt().toChar())
        return Pair(salt, SecretKeySpec(secret.encoded, ALGORITHM_AES))
    }

    private fun createEncryptedFile(
        inputStreamProvider: () -> InputStream,
        outputStreamProvider: () -> OutputStream,
        cipher: Cipher,
        cbcInitializationVector: ByteArray,
        salt: ByteArray
    ) {
        val input = inputStreamProvider()
        val output = outputStreamProvider()
        output.write(salt)
        output.write(cbcInitializationVector)
        val cos = CipherOutputStream(output, cipher)
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        generateSequence { input.read(buffer) }
            .takeWhile { it != EOF }
            .forEach { cos.write(buffer, NO_OFFSET, it) }
        cos.flush()
        cos.close()
        input.close()
    }

    override fun decrypt(
        password: CharArray,
        sourceStreamProvider: () -> InputStream,
        destinationStreamProvider: () -> OutputStream
    ) = sourceStreamProvider().use { readStream ->
        destinationStreamProvider().use { writeStream ->
            val salt: ByteArray = ByteArray(SALT_SIZE).apply { readStream.read(this) }
            val iv: ByteArray = ByteArray(IV_SIZE).apply { readStream.read(this) }
            val key = deriveKey(password.copyOf(), salt)
            decipherFile(key, iv, readStream, writeStream)
        }
    }

    private fun decipherFile(
        sks: SecretKeySpec,
        iv: ByteArray,
        sourceStream: InputStream,
        destinationStream: OutputStream
    ) {
        val cipher = Cipher.getInstance(CIPHER_AES_TRANSFORMATION)
            .apply { init(Cipher.DECRYPT_MODE, sks, IvParameterSpec(iv)) }
        val input = CipherInputStream(sourceStream, cipher)
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        generateSequence { input.read(buffer) }
            .takeWhile { it != EOF }
            .forEach { destinationStream.write(buffer, 0, it) }
        destinationStream.flush()
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(
            if (password.isEmpty()) charArrayOf('_') else password,
            salt,
            KEY_HASHING_TIMES,
            BIT_KEY_SIZE
        )
        val secret: SecretKey =
            SecretKeyFactory.getInstance(SECRET_KEYGEN_ALGORITHM).generateSecret(spec)
        return SecretKeySpec(secret.encoded, ALGORITHM_AES)
    }

    private companion object {
        private const val ALGORITHM_AES = "AES"
        private const val CIPHER_AES_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val SECRET_KEYGEN_ALGORITHM = "PBKDF2withHmacSHA1"
        private const val KEY_SIZE = 32
        private const val BYTE_SIZE = 8
        private const val BIT_KEY_SIZE = KEY_SIZE * BYTE_SIZE
        private const val STREAM_BUFFER_SIZE = 1024
        private const val EOF = -1
        private const val NO_OFFSET = 0
        private const val NULL_BYTE: Byte = 0x00
        private const val IV_SIZE = 16
        private const val SALT_SIZE = 16
        private const val KEY_HASHING_TIMES = 10000
    }

}
