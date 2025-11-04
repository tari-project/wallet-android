package com.tari.android.wallet.data.exolix

import android.os.Parcelable
import com.google.gson.Gson
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.util.extension.switchToIo
import kotlinx.parcelize.Parcelize
import retrofit2.HttpException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton


val TARI_CURRENCY = CurrencyDto(
    currency = Exolix.Currency(
//    code = "XTM",
        code = "ETH", // TODO uncomment!!!
        name = "Tari",
        icon = "https://exolix.com/icons/networks/Tari_1755361659372.png",
        notes = "",
    ),
    network = Exolix.Network(
        name = "Tari",
//    network = "Tari",
        network = "ETH", // TODO uncomment!!!
        icon = "https://exolix.com/icons/networks/Tari_1755361659372.png",
        isDefault = true,
        memoNeeded = false,
        precision = 10,
    ),
    selectable = false,
)

@Singleton
class ExolixRepository @Inject constructor(
    private val exolixRetrofitService: ExolixRetrofitService,
    private val corePrefRepository: CorePrefRepository,
) {

    suspend fun getCurrencies(
        page: Int? = null,
        size: Int? = null,
        searchQuery: String? = null,
    ): Result<CurrenciesResult> = switchToIo {
        runCatching {
            val response = exolixRetrofitService.getCurrencies(page, size, searchQuery, withNetworks = true)

            CurrenciesResult(
                currencies = response.data.flatMap { currency ->
                    currency.networks.orEmpty()
                        .sortedByDescending { it.isDefault }
                        .map { network -> CurrencyDto(currency, network) }
                },
                count = response.count,
            )
        }
    }

    suspend fun getCurrencyNetworks(code: String): Result<List<Exolix.Network>> = switchToIo {
        runCatching {
            exolixRetrofitService.getCurrencyNetworks(code)
        }
    }

    suspend fun getAllNetworks(
        page: Int? = null,
        size: Int? = null,
        search: String? = null
    ): Result<Exolix.NetworksResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.getAllNetworks(page, size, search)
        }
    }

    suspend fun getRate(
        coinFrom: String,
        networkFrom: String? = null,
        coinTo: String,
        networkTo: String? = null,
        amount: String,
        withdrawalAmount: String? = null,
        rateType: Exolix.RateType,
    ): Result<Exolix.Rate> = switchToIo {
        runCatching {
            exolixRetrofitService.getRate(coinFrom, networkFrom, coinTo, networkTo, amount, withdrawalAmount, rateType)
        }.recoverCatching { e ->
            if (e is HttpException && e.code() == 422) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorRate = Gson().fromJson(errorBody, Exolix.Rate::class.java)
                Exolix.Rate(
                    fromAmount = errorRate.fromAmount,
                    toAmount = errorRate.toAmount,
                    rate = BigDecimal.ZERO, // Not provided in error
                    message = errorRate.message,
                    minAmount = errorRate.minAmount,
                    withdrawMin = BigDecimal.ZERO, // Not provided in error
                    maxAmount = errorRate.maxAmount
                )
            } else {
                throw e
            }
        }
    }

    suspend fun getTransactions(
        page: Int? = null,
        size: Int? = null,
        search: String? = null,
        sort: String? = null,
        order: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        statuses: String? = null
    ): Result<Exolix.TransactionsResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.getTransactions(page, size, search, sort, order, dateFrom, dateTo, statuses)
        }
    }

    // Exchange methods
    suspend fun getTransaction(id: String): Result<Exolix.TransactionResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.getTransaction(id)
        }
    }

    suspend fun createExchange(requestData: ExchangeRequestData): Result<Exolix.CreateExchangeResponse> = switchToIo {
        runCatching {
            val toAddress = if (requestData.direction == ExchangeDirection.SELL_TARI) {
                requestData.selectedAddress ?: error("Selected address is null for SELL_TARI exchange")
            } else {
                corePrefRepository.walletAddress.fullBase58
            }
            val fromCurrency = if (requestData.direction == ExchangeDirection.SELL_TARI) TARI_CURRENCY else requestData.selectedCurrency
            val toCurrency = if (requestData.direction == ExchangeDirection.SELL_TARI) requestData.selectedCurrency else TARI_CURRENCY

            exolixRetrofitService.createExchange(
                Exolix.CreateExchangeRequest(
                    coinFrom = fromCurrency.coin,
                    networkFrom = fromCurrency.networkName,
                    coinTo = toCurrency.coin,
                    networkTo = toCurrency.networkName,
                    amount = requestData.amount,
                    withdrawalAddress = toAddress,
                )
            )
        }
    }
}

@Parcelize
data class CurrencyDto(
    val currency: Exolix.Currency,
    val network: Exolix.Network,
    val selectable: Boolean = true, // Tari currency is always selected and cannot be changed
) : Parcelable {
    val coin: String
        get() = currency.code

    val networkName: String
        get() = network.network

    val iconUrl: String
        get() = currency.icon

    override fun equals(other: Any?): Boolean {
        return other is CurrencyDto &&
                other.currency.code == currency.code &&
                other.network.network == network.network
    }
}

@Parcelize
data class ExchangeRequestData(
    val selectedCurrency: CurrencyDto,
    val selectedAddress: String? = null,
    val amount: BigDecimal,
    val rate: Exolix.Rate,
    val direction: ExchangeDirection,
) : Parcelable

data class CurrenciesResult(
    val currencies: List<CurrencyDto>,
    val count: Int,
)

enum class ExchangeDirection {
    SELL_TARI,
    BUY_TARI;
}