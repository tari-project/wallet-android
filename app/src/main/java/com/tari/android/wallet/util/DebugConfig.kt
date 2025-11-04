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
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.data.exolix.Exolix
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
import java.math.BigDecimal
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

    val showCopySeedsButton = valueIfDebug(true)

    val sweepFundsButtonEnabled = valueIfDebug(false)

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

    fun createNetwork(
        network: String = "ETH",
        name: String = "Ethereum",
        shortName: String? = "ETH",
        isDefault: Boolean = true,
        memoNeeded: Boolean = false,
        precision: Int = 18,
        icon: String? = "https://exolix.com/icons/coins/ETH.png",
        addressRegex: String? = "^0x[a-fA-F0-9]{40}$",
    ) = Exolix.Network(
        network = network,
        name = name,
        shortName = shortName,
        isDefault = isDefault,
        memoNeeded = memoNeeded,
        precision = precision,
        icon = icon,
        addressRegex = addressRegex,
    )

    fun createNetworkList() = listOf(
        createNetwork(
            network = "ETH",
            name = "Ethereum",
            shortName = "ETH",
            isDefault = true,
        ),
        createNetwork(
            network = "BSC",
            name = "Binance Smart Chain",
            shortName = "BSC",
            isDefault = false,
        ),
        createNetwork(
            network = "POLYGON",
            name = "Polygon",
            shortName = "MATIC",
            isDefault = false,
        ),
    )

    fun createCurrency(
        code: String = "ETH",
        name: String = "Ethereum",
        icon: String = "https://exolix.com/icons/coins/ETH.png",
        networks: List<Exolix.Network>? = createNetworkList(),
    ) = Exolix.Currency(
        code = code,
        name = name,
        icon = icon,
        notes = "Popular cryptocurrency",
        networks = networks,
    )

    fun createCurrencyDto(
        code: String = "ETH",
        name: String = "Ethereum",
        icon: String = "https://exolix.com/icons/coins/ETH.png",
        networks: List<Exolix.Network>? = createNetworkList(),
    ): CurrencyDto {
        val currency = createCurrency(
            code = code,
            name = name,
            icon = icon,
            networks = networks,
        )
        val network = currency.networks!!.first { it.isDefault }
        return CurrencyDto(
            currency = currency,
            network = network,
        )
    }

    fun createCurrencyList(count: Int = 10) = List(count) { index ->
        val currencies = listOf(
            Triple("ETH", "Ethereum", "https://exolix.com/icons/coins/ETH.png"),
            Triple("BTC", "Bitcoin", "https://exolix.com/icons/coins/BTC.png"),
            Triple("USDT", "Tether", "https://exolix.com/icons/coins/USDT.png"),
            Triple("BNB", "Binance Coin", "https://exolix.com/icons/coins/BNB.png"),
            Triple("ADA", "Cardano", "https://exolix.com/icons/coins/ADA.png"),
            Triple("SOL", "Solana", "https://exolix.com/icons/coins/SOL.png"),
            Triple("DOT", "Polkadot", "https://exolix.com/icons/coins/DOT.png"),
            Triple("DOGE", "Dogecoin", "https://exolix.com/icons/coins/DOGE.png"),
            Triple("AVAX", "Avalanche", "https://exolix.com/icons/coins/AVAX.png"),
            Triple("LTC", "Litecoin", "https://exolix.com/icons/coins/LTC.png"),
        )
        val (code, name, icon) = currencies[index % currencies.size]
        createCurrency(
            code = code,
            name = name,
            icon = icon,
            networks = if (index % 3 == 0) createNetworkList() else listOf(createNetwork(code, name, code, true)),
        )
    }

    fun createCurrencyDtoList(count: Int = 10): List<CurrencyDto> {
        val currencies = listOf(
            Triple("ETH", "Ethereum", "https://exolix.com/icons/coins/ETH.png"),
            Triple("BTC", "Bitcoin", "https://exolix.com/icons/coins/BTC.png"),
            Triple("USDT", "Tether", "https://exolix.com/icons/coins/USDT.png"),
            Triple("BNB", "Binance Coin", "https://exolix.com/icons/coins/BNB.png"),
            Triple("ADA", "Cardano", "https://exolix.com/icons/coins/ADA.png"),
            Triple("SOL", "Solana", "https://exolix.com/icons/coins/SOL.png"),
            Triple("DOT", "Polkadot", "https://exolix.com/icons/coins/DOT.png"),
            Triple("DOGE", "Dogecoin", "https://exolix.com/icons/coins/DOGE.png"),
            Triple("AVAX", "Avalanche", "https://exolix.com/icons/coins/AVAX.png"),
            Triple("LTC", "Litecoin", "https://exolix.com/icons/coins/LTC.png"),
        )

        return buildList {
            for (index in 0 until count) {
                val (code, name, icon) = currencies[index % currencies.size]
                val currency = createCurrency(
                    code = code,
                    name = name,
                    icon = icon,
                    networks = if (index % 3 == 0) createNetworkList() else listOf(createNetwork(code, name, code, true)),
                )

                val networks = currency.networks.orEmpty()
                val sortedNetworks = networks.sortedByDescending { it.isDefault }

                sortedNetworks.forEach { network -> add(CurrencyDto(currency, network)) }
            }
        }
    }

    fun createRate(
        fromAmount: BigDecimal = 100.0.toBigDecimal(),
        toAmount: BigDecimal = 95.toBigDecimal(),
        rate: BigDecimal = 0.95.toBigDecimal(),
        message: String? = null,
        minAmount: BigDecimal = 10.0.toBigDecimal(),
        withdrawMin: BigDecimal = 5.0.toBigDecimal(),
        maxAmount: BigDecimal = 1000.0.toBigDecimal(),
    ) = Exolix.Rate(
        fromAmount = fromAmount,
        toAmount = toAmount,
        rate = rate,
        message = message,
        minAmount = minAmount,
        withdrawMin = withdrawMin,
        maxAmount = maxAmount,
    )

    fun createCoinInfo(
        coinCode: String = "ETH",
        coinName: String = "Ethereum",
        network: String = "ethereum",
        networkName: String = "Ethereum",
        networkShortName: String? = "ETH",
        icon: String = "https://exolix.com/icons/coins/ETH.png",
    ) = Exolix.CoinInfo(
        coinCode = coinCode,
        coinName = coinName,
        network = network,
        networkName = networkName,
        networkShortName = networkShortName,
        icon = icon,
    )

    fun createTransactionResponse(
        id: String = "test-transaction-id-12345",
        amount: BigDecimal = BigDecimal("100.01"),
        amountTo: BigDecimal = BigDecimal("0.9982"),
        coinFrom: Exolix.CoinInfo = createCoinInfo(
            coinCode = "ETH",
            coinName = "Ethereum",
            network = "ethereum",
            networkName = "Ethereum",
            networkShortName = "ETH",
        ),
        coinTo: Exolix.CoinInfo = createCoinInfo(
            coinCode = "XTM",
            coinName = "Tari",
            network = "tari",
            networkName = "Tari",
            networkShortName = "XTM",
        ),
        createdAt: String = "2024-01-01T12:00:00Z",
        depositAddress: String = "0x1234567890abcdef1234567890abcdef12345678",
        depositExtraId: String? = null,
        withdrawalAddress: String = "0x9876543210fedcba9876543210fedcba98765432",
        withdrawalExtraId: String? = null,
        hashIn: Exolix.HashInfo? = null,
        hashOut: Exolix.HashInfo? = null,
        rate: BigDecimal = BigDecimal("0.01"),
        rateType: Exolix.RateType = Exolix.RateType.FIXED,
        refundAddress: String? = null,
        refundExtraId: String? = null,
        status: Exolix.TransactionStatus = Exolix.TransactionStatus.SUCCESS,
        source: String? = null,
        comment: String? = null,
    ) = Exolix.TransactionResponse(
        id = id,
        amount = amount,
        amountTo = amountTo,
        coinFrom = coinFrom,
        coinTo = coinTo,
        comment = comment,
        createdAt = createdAt,
        depositAddress = depositAddress,
        depositExtraId = depositExtraId,
        withdrawalAddress = withdrawalAddress,
        withdrawalExtraId = withdrawalExtraId,
        hashIn = hashIn,
        hashOut = hashOut,
        rate = rate,
        rateType = rateType,
        refundAddress = refundAddress,
        refundExtraId = refundExtraId,
        status = status,
        source = source,
    )
}

object YatEnvironment {
    val SANDBOX = YatIntegration.Environment("https://a.yat.fyi/", "https://yat.fyi/")
    val PRODUCTION = YatIntegration.Environment("https://a.y.at/", "https://y.at/")
}