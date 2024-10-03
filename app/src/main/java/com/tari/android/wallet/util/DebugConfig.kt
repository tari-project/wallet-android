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
@file:Suppress("MemberVisibilityCanBePrivate", "ConstPropertyName", "KotlinConstantConditions", "SameParameterValue")

package com.tari.android.wallet.util

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.extension.minusHours
import com.tari.android.wallet.extension.toMicroTari
import com.tari.android.wallet.ffi.Base58
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.common.gyphy.presentation.GifViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GifRepository
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.fragment.chat.data.ChatItemDto
import com.tari.android.wallet.ui.fragment.chat.data.ChatMessageItemDto
import com.tari.android.wallet.ui.fragment.contactBook.address_poisoning.SimilarAddressDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosViewHolderItem
import org.joda.time.DateTime
import yat.android.lib.YatIntegration
import java.math.BigInteger
import java.util.Date
import java.util.UUID
import kotlin.random.Random

/**
 *  Constants used for developing and debugging.
 */
object DebugConfig {

    val mockUtxos = valueIfDebug(false)

    val mockTxs = valueIfDebug(false)

    val mockChatMessages = valueIfDebug(false)

    val suppressAddAmountErrors = valueIfDebug(false)

    val mockSeedPhraseSorting = valueIfDebug(false)

    val mockEveryAddressPoisoned: Boolean = valueIfDebug(false)
    val mockPoisonedAddresses: Boolean = valueIfDebug(false)

    const val isChatEnabled = false

    const val isYatEnabled = false
    private val _useYatSandbox = valueIfDebug(false)
    val yatEnvironment = if (_useYatSandbox) YatEnvironment.SANDBOX else YatEnvironment.PRODUCTION

    val mockNetwork = valueIfDebug(true)

    val hardcodedBaseNodes = valueIfDebug(false)

    val showCopySeedsButton = valueIfDebug(true)

    private fun isDebug() = BuildConfig.BUILD_TYPE == "debug"

    private fun valueIfDebug(value: Boolean) = isDebug() && value
}

object MockDataStub {
    private const val EMOJI_ID: EmojiId =
        "\uD83C\uDF34\uD83C\uDF0D\uD83C\uDFB5\uD83C\uDFBA\uD83D\uDDFD\uD83C\uDF37\uD83D\uDE91\uD83C\uDF45\uD83D\uDC60\uD83C\uDF1F\uD83D\uDC8C\uD83D\uDE97\uD83D\uDC40\uD83D\uDD29\uD83C\uDF08\uD83D\uDC1D\uD83C\uDF37\uD83C\uDF70\uD83C\uDF38\uD83C\uDF81\uD83C\uDF55\uD83D\uDEBF\uD83D\uDC34\uD83D\uDCA6\uD83D\uDE0E\uD83D\uDEAA\uD83C\uDFE0\uD83D\uDD29\uD83C\uDFE0\uD83D\uDE82\uD83C\uDFBA\uD83C\uDFC6\uD83C\uDFB3"
    private const val BASE58: Base58 = "C05575BE00EF016A209B1F493D9027B0E330F3E25FE89BBE6FA66D966EE5B6356"

    // TODO make better mock stub
    val WALLET_ADDRESS = TariWalletAddress(
        network = TariWalletAddress.Network.NEXTNET,
        features = listOf(TariWalletAddress.Feature.INTERACTIVE),
        networkEmoji = EMOJI_ID,
        featuresEmoji = EMOJI_ID,
        viewKeyEmojis = EMOJI_ID,
        spendKeyEmojis = EMOJI_ID,
        checksumEmoji = EMOJI_ID,
        fullBase58 = BASE58,
        fullEmojiId = EMOJI_ID,
        unknownAddress = false,
    )

    private val RANDOM_MESSAGES = listOf(
        "Hello, how are you?",
        "I'm fine, thank you!",
        "What are you doing?",
        "I'm working on a new feature.",
        "That's great!",
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
        "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
        "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
        "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur.",
    )

    fun createContact(
        walletAddress: TariWalletAddress = WALLET_ADDRESS,
        alias: String = "Alice",
    ) = ContactDto(
        contactInfo = FFIContactInfo(
            walletAddress = walletAddress,
            alias = alias,
        ),
    )

    fun createUtxoList() = List(20) {
        UtxosViewHolderItem(
            source = createUtxo(),
            networkBlockHeight = 10000L,
        )
    }

    fun createUtxo() = TariUtxo(
        value = (Random.nextLong(1, 100000) * 10000).toMicroTari(),
        status = TariUtxo.UtxoStatus.entries.toTypedArray()[Random.nextInt(0, 3)],
        timestamp = DateTime.now().toDate().time,
        minedHeight = 12243,
        lockHeight = Random.nextLong(1, 12243),
        commitment = "Mocked Tari UTXO!!!",
    )

    fun createTxList(
        gifRepository: GifRepository,
        confirmationCount: Long,
        title: String = "Mocked Transactions"
    ) = listOf(
        TitleViewHolderItem(title = title, isFirst = true),
        createTx(
            gifRepository, confirmationCount,
            amount = 1100000,
            contactAlias = "Alice",
        ),
        createTx(
            gifRepository, confirmationCount,
            amount = 1200000,
            contactAlias = "Bob",
        ),
        createTx(
            gifRepository, confirmationCount,
            amount = 1300000,
            contactAlias = "Charlie",
        ),
        createTx(
            gifRepository, confirmationCount,
            amount = 1400000,
            contactAlias = "David",
            status = TxStatus.COINBASE,
        ),
    )

    fun createTx(
        gifRepository: GifRepository,
        confirmationCount: Long,
        amount: Long = 100000,
        contactAlias: String = "Test",
        status: TxStatus = TxStatus.MINED_CONFIRMED,
    ) = TransactionItem(
        tx = CompletedTx(
            direction = Tx.Direction.INBOUND,
            status = status,
            amount = amount.toMicroTari(),
            fee = 1000.toMicroTari(),
            message = "https://giphy.com/embed/5885nYOgBHdCw",
            timestamp = BigInteger.valueOf(System.currentTimeMillis()),
            id = 1.toBigInteger(),
            tariContact = TariContact(WALLET_ADDRESS, contactAlias),
        ),
        contact = createContact(alias = contactAlias),
        position = 0,
        gifViewModel = GifViewModel(gifRepository),
        requiredConfirmationCount = confirmationCount,
    )

    fun createSimilarAddressList() = listOf(
        createSimilarAddress(),
        createSimilarAddress().copy(trusted = true),
        createSimilarAddress(),
    )

    fun createSimilarAddress(): SimilarAddressDto {
        return SimilarAddressDto(
            contactDto = createContact(),
            numberOfTransaction = 10,
            lastTransactionTimestampMillis = System.currentTimeMillis(),
            trusted = false,
        )
    }

    fun createChatList(count: Int = 20) = List(count) {
        ChatItemDto(
            uuid = UUID.randomUUID().toString(),
            messages = createChatMessages(Random.nextInt(1, 20)),
            walletAddress = WALLET_ADDRESS,
        )
    }

    fun createChatMessages(count: Int = 20) = List(count) {
        ChatMessageItemDto(
            message = RANDOM_MESSAGES.random(),
            date = Date().minusHours(Random.nextInt(0, 100)),
            isMine = Random.nextBoolean(),
            isRead = Random.nextBoolean(0.9),
        )
    }
}

fun Random.nextBoolean(probability: Double): Boolean = nextDouble() <= probability

object YatEnvironment {
    val SANDBOX = YatIntegration.Environment("https://a.yat.fyi/", "https://yat.fyi/")
    val PRODUCTION = YatIntegration.Environment("https://a.y.at/", "https://y.at/")
}