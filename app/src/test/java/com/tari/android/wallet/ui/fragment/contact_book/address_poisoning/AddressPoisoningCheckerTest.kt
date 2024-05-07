package com.tari.android.wallet.ui.fragment.contact_book.address_poisoning

import com.tari.android.wallet.model.TariWalletAddress
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressPoisoningCheckerTest {

    private fun TariWalletAddress.emojiIdSymbols(): List<String> = emojiId.map { it.toString() }

    @Test
    fun `assert that same texts`() {
        val firstAddress = TariWalletAddress(emojiId = "123xxxabc")
        val secondAddress = TariWalletAddress(emojiId = "123xxxabc")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertTrue(result)
    }

    @Test
    fun `assert that similar texts`() {
        val firstAddress = TariWalletAddress(emojiId = "123xxxabc")
        val secondAddress = TariWalletAddress(emojiId = "123xxxdef")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertTrue(result)
    }

    @Test
    fun `assert that different texts`() {
        val firstAddress = TariWalletAddress(emojiId = "123xxxabc")
        val secondAddress = TariWalletAddress(emojiId = "a23xxxdef")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertFalse(result)
    }

    @Test
    fun `assert that texts with different lengths`() {
        val firstAddress = TariWalletAddress(emojiId = "123xxxabc")
        val secondAddress = TariWalletAddress(emojiId = "123xxxxdef")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertFalse(result)
    }

    @Test
    fun `assert that texts with zero same characters`() {
        val firstAddress = TariWalletAddress(emojiId = "123xxxabc")
        val secondAddress = TariWalletAddress(emojiId = "123xxxdef")
        val minSameCharacters = 0
        val usedPrefixSuffixCharacters = 3

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertTrue(result)
    }

    @Test
    fun `assert that empty address`() {
        val firstAddress = TariWalletAddress(emojiId = "")
        val secondAddress = TariWalletAddress(emojiId = "123xxxdef")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertFalse(result)
    }

    @Test
    fun `assert that empty addresses`() {
        val firstAddress = TariWalletAddress(emojiId = "")
        val secondAddress = TariWalletAddress(emojiId = "")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertFalse(result)
    }

    @Test
    fun `assert that too short address`() {
        val firstAddress = TariWalletAddress(emojiId = "123bc")
        val secondAddress = TariWalletAddress(emojiId = "123xxxdef")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertFalse(result)
    }

    @Test
    fun `assert that too short addresses`() {
        val firstAddress = TariWalletAddress(emojiId = "123bc")
        val secondAddress = TariWalletAddress(emojiId = "123ef")

        val result = firstAddress.emojiIdSymbols().isSimilarEmojiId(secondAddress.emojiIdSymbols())

        assertFalse(result)
    }
}
