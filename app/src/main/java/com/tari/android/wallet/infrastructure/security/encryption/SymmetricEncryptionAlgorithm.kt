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

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.*
import javax.crypto.*
import javax.crypto.spec.GCMParameterSpec
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

/**
 * Password based file encryption.
 *
 * Current format (written by [encrypt]):
 * `MAGIC (8 bytes) | salt (16 bytes) | nonce (12 bytes) | AES-256-GCM ciphertext and tag`,
 * key = PBKDF2-HMAC-SHA256(password, salt, 200,000 iterations), the magic is authenticated as associated data.
 *
 * Legacy format (still read by [decrypt], no longer written):
 * `salt (16 bytes) | iv (16 bytes) | AES-256-CBC/PKCS5 ciphertext`,
 * key = PBKDF2-HMAC-SHA1(password, salt, 10,000 iterations), no integrity protection.
 */
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
        val salt = ByteArray(SALT_SIZE).apply(random::nextBytes)
        val nonce = ByteArray(GCM_NONCE_SIZE).apply(random::nextBytes)
        val key = deriveKey(password.copyOf(), salt, KEYGEN_ALGORITHM, KEY_HASHING_TIMES)
        val cipher = Cipher.getInstance(CIPHER_GCM_TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
            updateAAD(MAGIC)
        }
        sourceStreamProvider().use { input ->
            destinationStreamProvider().use { output ->
                output.write(MAGIC)
                output.write(salt)
                output.write(nonce)
                transform(cipher, input, output)
            }
        }
    }

    override fun decrypt(
        password: CharArray,
        sourceStreamProvider: () -> InputStream,
        destinationStreamProvider: () -> OutputStream
    ) = sourceStreamProvider().use { readStream ->
        destinationStreamProvider().use { writeStream ->
            val prefix = readStream.readExactly(MAGIC.size)
            if (prefix.contentEquals(MAGIC)) {
                val salt = readStream.readExactly(SALT_SIZE)
                val nonce = readStream.readExactly(GCM_NONCE_SIZE)
                val key = deriveKey(password.copyOf(), salt, KEYGEN_ALGORITHM, KEY_HASHING_TIMES)
                val cipher = Cipher.getInstance(CIPHER_GCM_TRANSFORMATION).apply {
                    init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, nonce))
                    updateAAD(MAGIC)
                }
                // A wrong password or a modified file fails the tag check in doFinal() with AEADBadTagException.
                transform(cipher, readStream, writeStream)
            } else {
                // Backup written before the format had a header: the bytes read so far are the start of the salt.
                val salt = prefix + readStream.readExactly(SALT_SIZE - prefix.size)
                val iv = readStream.readExactly(IV_SIZE)
                val key = deriveKey(password.copyOf(), salt, LEGACY_KEYGEN_ALGORITHM, LEGACY_KEY_HASHING_TIMES)
                decipherLegacyFile(key, iv, readStream, writeStream)
            }
        }
    }

    private fun transform(cipher: Cipher, input: InputStream, output: OutputStream) {
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        generateSequence { input.read(buffer) }
            .takeWhile { it != EOF }
            .forEach { read -> cipher.update(buffer, NO_OFFSET, read)?.let(output::write) }
        cipher.doFinal()?.let(output::write)
        output.flush()
    }

    private fun decipherLegacyFile(
        sks: SecretKeySpec,
        iv: ByteArray,
        sourceStream: InputStream,
        destinationStream: OutputStream
    ) {
        val cipher = Cipher.getInstance(LEGACY_CIPHER_TRANSFORMATION)
            .apply { init(Cipher.DECRYPT_MODE, sks, IvParameterSpec(iv)) }
        val input = CipherInputStream(sourceStream, cipher)
        val buffer = ByteArray(STREAM_BUFFER_SIZE)
        generateSequence { input.read(buffer) }
            .takeWhile { it != EOF }
            .forEach { destinationStream.write(buffer, 0, it) }
        destinationStream.flush()
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, algorithm: String, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(
            if (password.isEmpty()) charArrayOf('_') else password,
            salt,
            iterations,
            BIT_KEY_SIZE
        )
        val secret: SecretKey = SecretKeyFactory.getInstance(algorithm).generateSecret(spec)
        spec.clearPassword()
        Arrays.fill(password, NULL_BYTE.toInt().toChar())
        return SecretKeySpec(secret.encoded, ALGORITHM_AES)
    }

    private fun InputStream.readExactly(size: Int): ByteArray {
        val bytes = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val read = read(bytes, offset, size - offset)
            if (read == EOF) throw EOFException("Encrypted file is too short.")
            offset += read
        }
        return bytes
    }

    private companion object {
        private const val ALGORITHM_AES = "AES"
        private const val CIPHER_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEYGEN_ALGORITHM = "PBKDF2withHmacSHA256"
        private const val KEY_HASHING_TIMES = 200_000
        private const val GCM_NONCE_SIZE = 12
        private const val GCM_TAG_BITS = 128
        private val MAGIC = "TARIBK02".toByteArray(Charsets.US_ASCII)

        private const val LEGACY_CIPHER_TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val LEGACY_KEYGEN_ALGORITHM = "PBKDF2withHmacSHA1"
        private const val LEGACY_KEY_HASHING_TIMES = 10000
        private const val IV_SIZE = 16

        private const val KEY_SIZE = 32
        private const val BYTE_SIZE = 8
        private const val BIT_KEY_SIZE = KEY_SIZE * BYTE_SIZE
        private const val STREAM_BUFFER_SIZE = 1024
        private const val EOF = -1
        private const val NO_OFFSET = 0
        private const val NULL_BYTE: Byte = 0x00
        private const val SALT_SIZE = 16
    }

}
