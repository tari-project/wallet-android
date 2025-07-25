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
import com.tari.android.wallet.application.addressPoisoning.SimilarAddressDto
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.model.Base58
import com.tari.android.wallet.model.CompletedTransactionKernel
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.ui.screen.utxos.list.adapters.UtxosViewHolderItem
import com.tari.android.wallet.util.extension.toMicroTari
import org.joda.time.DateTime
import yat.android.lib.YatIntegration
import java.math.BigInteger
import kotlin.random.Random

/**
 *  Constants used for developing and debugging.
 */
object DebugConfig {

    val mockUtxos = valueIfDebug(false)

    val mockSeedPhraseSorting = valueIfDebug(false)

    val mockEveryAddressPoisoned: Boolean = valueIfDebug(false)
    val mockPoisonedAddresses: Boolean = valueIfDebug(false)

    const val isYatEnabled = false
    private val _useYatSandbox = valueIfDebug(false)
    val yatEnvironment = if (_useYatSandbox) YatEnvironment.SANDBOX else YatEnvironment.PRODUCTION

    val hardcodedBaseNodes = valueIfDebug(false)

    val showCopySeedsButton = valueIfDebug(true)

    val sweepFundsButtonEnabled = valueIfDebug(false)

    val selectBaseNodeEnabled = valueIfDebug(false) // TODO remove all the code related to this ?

    const val showInvitedFriendsInProfile = false

    const val showTtlStoreMenu = false

    fun isDebug() = BuildConfig.BUILD_TYPE == "debug"

    private fun valueIfDebug(value: Boolean) = isDebug() && value
}

object MockDataStub {
    private const val EMOJI_ID: EmojiId =
        "\uD83C\uDF34\uD83C\uDF0D\uD83C\uDFB5\uD83C\uDFBA\uD83D\uDDFD\uD83C\uDF37\uD83D\uDE91\uD83C\uDF45\uD83D\uDC60\uD83C\uDF1F\uD83D\uDC8C\uD83D\uDE97\uD83D\uDC40\uD83D\uDD29\uD83C\uDF08\uD83D\uDC1D\uD83C\uDF37\uD83C\uDF70\uD83C\uDF38\uD83C\uDF81\uD83C\uDF55\uD83D\uDEBF\uD83D\uDC34\uD83D\uDCA6\uD83D\uDE0E\uD83D\uDEAA\uD83C\uDFE0\uD83D\uDD29\uD83C\uDFE0\uD83D\uDE82\uD83C\uDFBA\uD83C\uDFC6\uD83C\uDFB3"
    private const val BASE58: Base58 = "C05575BE00EF016A209B1F493D9027B0E330F3E25FE89BBE6FA66D966EE5B6356"

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

    private val RANDOM_NAMES = listOf(
        "Alice",
        "Bob",
        "Charlie",
        "David",
        "Eve",
        "Frank",
        "Grace",
        "Heidi",
        "Ivan",
        "Judy",
    )

    fun createContact(
        walletAddress: TariWalletAddress = WALLET_ADDRESS,
        alias: String = "Alice",
    ) = Contact(
        walletAddress = walletAddress,
        alias = alias,
    )

    fun createContactList(count: Int = 20) = List(count) {
        createContact(
            walletAddress = WALLET_ADDRESS.copy(
                fullBase58 = WALLET_ADDRESS.fullBase58 + it,
                fullEmojiId = WALLET_ADDRESS.fullEmojiId + it,
            ),
            alias = RANDOM_NAMES.random() + " $it",
        )
    }

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

    fun createTxList() = listOf(
        createTxDto(
            amount = 1000000,
            contactAlias = "Alice",
        ),
        createTxDto(
            amount = 2000000,
            contactAlias = "Bob",
        ),
        createTxDto(
            amount = 3000000,
            contactAlias = "Charlie",
        ),
        createTxDto(
            amount = 4000000,
            contactAlias = "David",
            status = TxStatus.COINBASE,
        ),
    )

    fun createCompletedTx(
        amount: Long = 100000,
        direction: Tx.Direction = Tx.Direction.OUTBOUND,
        contactAlias: String = "Test",
        status: TxStatus = TxStatus.MINED_CONFIRMED,
    ) = CompletedTx(
        direction = direction,
        status = status,
        amount = amount.toMicroTari(),
        fee = 1000.toMicroTari(),
        paymentId = RANDOM_MESSAGES.random(),
        timestamp = BigInteger.valueOf(System.currentTimeMillis()),
        id = 1.toBigInteger(),
        tariContact = TariContact(WALLET_ADDRESS, contactAlias),
        confirmationCount = 0.toBigInteger(),
        txKernel = CompletedTransactionKernel(
            excess = "excess",
            publicNonce = "publicNonce",
            signature = "signature",
        ),
        minedTimestamp = BigInteger.valueOf(System.currentTimeMillis()),
        minedHeight = 0.toBigInteger(),
    )

    fun createCancelledTx(
        amount: Long = 100000,
        direction: Tx.Direction = Tx.Direction.OUTBOUND,
        contactAlias: String = "Test",
        status: TxStatus = TxStatus.UNKNOWN,
    ) = CancelledTx(
        id = 1.toBigInteger(),
        direction = direction,
        amount = amount.toMicroTari(),
        timestamp = BigInteger.valueOf(System.currentTimeMillis()),
        paymentId = RANDOM_MESSAGES.random(),
        status = status,
        tariContact = TariContact(WALLET_ADDRESS, contactAlias),
        fee = 1000.toMicroTari(),
        cancellationReason = FFITxCancellationReason.UserCancelled,
    )

    fun createPendingTx(
        amount: Long = 100000,
        direction: Tx.Direction = Tx.Direction.OUTBOUND,
        contactAlias: String = "Test",
        status: TxStatus = TxStatus.PENDING,
    ) = PendingOutboundTx(
        id = 1.toBigInteger(),
        direction = direction,
        amount = amount.toMicroTari(),
        timestamp = BigInteger.valueOf(System.currentTimeMillis()),
        paymentId = RANDOM_MESSAGES.random(),
        status = status,
        tariContact = TariContact(WALLET_ADDRESS, contactAlias),
        fee = 1000.toMicroTari(),
    )

    fun createTxDto(
        amount: Long = 100000,
        contactAlias: String = "Test",
        status: TxStatus = TxStatus.MINED_CONFIRMED,
    ) = TxDto(
        tx = createCompletedTx(
            amount = amount,
            contactAlias = contactAlias,
            status = status,
        ),
        contact = createContact(alias = contactAlias),
    )

    fun createSimilarAddressList() = listOf(
        createSimilarAddress(),
        createSimilarAddress().copy(trusted = true),
        createSimilarAddress(),
    )

    fun createSimilarAddress(): SimilarAddressDto {
        return SimilarAddressDto(
            contact = createContact(),
            numberOfTransaction = 10,
            lastTransactionTimestampMillis = System.currentTimeMillis(),
            trusted = false,
        )
    }
}

object YatEnvironment {
    val SANDBOX = YatIntegration.Environment("https://a.yat.fyi/", "https://yat.fyi/")
    val PRODUCTION = YatIntegration.Environment("https://a.y.at/", "https://y.at/")
}