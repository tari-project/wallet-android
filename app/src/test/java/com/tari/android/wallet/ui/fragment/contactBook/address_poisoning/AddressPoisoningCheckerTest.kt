package com.tari.android.wallet.ui.fragment.contactBook.address_poisoning

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.util.MockDataStub

class AddressPoisoningCheckerTest {

    private fun TariWalletAddress.emojiIdSymbols(): List<String> = fullEmojiId.map { it.toString() }

    private fun createAddress(emojiId: EmojiId): TariWalletAddress = MockDataStub.WALLET_ADDRESS.copy(spendKeyEmojis = emojiId)

    // TODO uncomment tests when the new wallet address feature is fully ready
//    @Test
//    fun `assert that same texts`() {
//        val firstAddress = createAddress(emojiId = "123xxxabc")
//        val secondAddress = createAddress(emojiId = "123xxxabc")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertTrue(result)
//    }
//
//    @Test
//    fun `assert that similar texts`() {
//        val firstAddress = createAddress(emojiId = "123xxxabc")
//        val secondAddress = createAddress(emojiId = "123xxxdef")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertTrue(result)
//    }
//
//    @Test
//    fun `assert that different texts`() {
//        val firstAddress = createAddress(emojiId = "123xxxabc")
//        val secondAddress = createAddress(emojiId = "a23xxxdef")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun `assert that texts with different lengths`() {
//        val firstAddress = createAddress(emojiId = "123xxxabc")
//        val secondAddress = createAddress(emojiId = "123xxxxdef")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun `assert that texts with zero same characters`() {
//        val firstAddress = createAddress(emojiId = "123xxxabc")
//        val secondAddress = createAddress(emojiId = "123xxxdef")
//        val minSameCharacters = 0
//        val usedPrefixSuffixCharacters = 3
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertTrue(result)
//    }
//
//    @Test
//    fun `assert that empty address`() {
//        val firstAddress = createAddress(emojiId = "")
//        val secondAddress = createAddress(emojiId = "123xxxdef")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun `assert that empty addresses`() {
//        val firstAddress = createAddress(emojiId = "")
//        val secondAddress = createAddress(emojiId = "")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun `assert that too short address`() {
//        val firstAddress = createAddress(emojiId = "123bc")
//        val secondAddress = createAddress(emojiId = "123xxxdef")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertFalse(result)
//    }
//
//    @Test
//    fun `assert that too short addresses`() {
//        val firstAddress = createAddress(emojiId = "123bc")
//        val secondAddress = createAddress(emojiId = "123ef")
//
//        val result = firstAddress.isSimilarTo(secondAddress)
//
//        assertFalse(result)
//    }
}
