package com.tari.android.wallet.infrastructure.security.encryption

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

class SymmetricEncryptionAlgorithmTest {

    private val plaintext = Random(1).nextBytes(100_000)

    private fun encrypt(password: String, data: ByteArray): ByteArray = ByteArrayOutputStream().also { out ->
        SymmetricEncryptionAlgorithm.aes().encrypt(password.toCharArray(), { ByteArrayInputStream(data) }, { out })
    }.toByteArray()

    private fun decrypt(password: String, data: ByteArray): ByteArray = ByteArrayOutputStream().also { out ->
        SymmetricEncryptionAlgorithm.aes().decrypt(password.toCharArray(), { ByteArrayInputStream(data) }, { out })
    }.toByteArray()

    /** The format written by the app before the header was introduced. */
    private fun encryptLegacy(password: String, data: ByteArray): ByteArray {
        val salt = ByteArray(16) { (it + 1).toByte() }
        val iv = ByteArray(16) { (it + 101).toByte() }
        val key = SecretKeyFactory.getInstance("PBKDF2withHmacSHA1")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, 10_000, 256)).encoded
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            .apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv)) }
        return salt + iv + cipher.doFinal(data)
    }

    @Test
    fun `encrypted data decrypts to the original with the same password`() {
        assertArrayEquals(plaintext, decrypt("correct horse", encrypt("correct horse", plaintext)))
    }

    @Test
    fun `encrypted data starts with the format header and carries salt, nonce and tag`() {
        val encrypted = encrypt("correct horse", plaintext)

        assertEquals("TARIBK02", String(encrypted.copyOfRange(0, 8), Charsets.US_ASCII))
        assertEquals(8 + 16 + 12 + plaintext.size + 16, encrypted.size)
    }

    @Test
    fun `two encryptions of the same data differ`() {
        assertFalse(encrypt("correct horse", plaintext).contentEquals(encrypt("correct horse", plaintext)))
    }

    @Test
    fun `a wrong password is rejected`() {
        val encrypted = encrypt("correct horse", plaintext)

        val error = assertThrows(AEADBadTagException::class.java) { decrypt("wrong horse", encrypted) }
        assertTrue(error is GeneralSecurityException)
    }

    @Test
    fun `a modified ciphertext is rejected and yields no output`() {
        val encrypted = encrypt("correct horse", plaintext)
        encrypted[encrypted.size / 2] = (encrypted[encrypted.size / 2].toInt() xor 1).toByte()
        val out = ByteArrayOutputStream()

        assertThrows(AEADBadTagException::class.java) {
            SymmetricEncryptionAlgorithm.aes().decrypt("correct horse".toCharArray(), { ByteArrayInputStream(encrypted) }, { out })
        }
        assertEquals(0, out.size())
    }

    @Test
    fun `a backup in the legacy format still decrypts`() {
        assertArrayEquals(plaintext, decrypt("correct horse", encryptLegacy("correct horse", plaintext)))
    }

    @Test
    fun `a legacy backup does not decrypt to the original with a wrong password`() {
        val legacy = encryptLegacy("correct horse", plaintext)

        val result = runCatching { decrypt("wrong horse", legacy) }

        assertFalse(result.getOrNull()?.contentEquals(plaintext) ?: false)
    }

    @Test
    fun `a truncated file is rejected`() {
        assertThrows(EOFException::class.java) { decrypt("correct horse", encrypt("correct horse", plaintext).copyOfRange(0, 20)) }
    }
}
