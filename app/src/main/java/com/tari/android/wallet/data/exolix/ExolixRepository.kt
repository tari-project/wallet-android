package com.tari.android.wallet.data.exolix

import android.os.Parcelable
import com.google.gson.Gson
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.exolix.ExolixPrefRepository
import com.tari.android.wallet.util.extension.switchToIo
import kotlinx.parcelize.Parcelize
import retrofit2.HttpException
import java.math.BigDecimal
import javax.inject.Inject
import javax.inject.Singleton

private const val TARI_CURRENCY_CODE = "XTM"

@Singleton
class ExolixRepository @Inject constructor(
    private val exolixRetrofitService: ExolixRetrofitService,
    private val corePrefRepository: CorePrefRepository,
    private val exolixPrefRepository: ExolixPrefRepository,
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

    suspend fun getTariCurrency(): Result<CurrencyDto> = switchToIo {
        runCatching {
            val currencyResponse = exolixRetrofitService.getCurrencies(search = TARI_CURRENCY_CODE, withNetworks = true)
            val tariCurrency = currencyResponse.data.firstOrNull()
                ?: error("Tari currency not found in Exolix response")
            val tariNetwork = tariCurrency.networks?.firstOrNull()
                ?: error("Tari network not found in Exolix response")

            CurrencyDto(
                currency = tariCurrency,
                network = tariNetwork,
                selectable = false,
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

    suspend fun getTransaction(id: String): Result<Exolix.Transaction> = switchToIo {
        runCatching {
            exolixRetrofitService.getTransaction(id)
        }
    }

    suspend fun createExchange(requestData: ExchangeRequestData): Result<Exolix.Transaction> = switchToIo {
        runCatching {
            val toAddress = if (requestData.direction == ExchangeDirection.SELL_TARI) {
                requestData.selectedAddress ?: error("Selected address is null for SELL_TARI exchange")
            } else {
                corePrefRepository.walletAddress.fullBase58
            }
            val fromCurrency = if (requestData.direction == ExchangeDirection.SELL_TARI) requestData.tariCurrency else requestData.selectedCurrency
            val toCurrency = if (requestData.direction == ExchangeDirection.SELL_TARI) requestData.selectedCurrency else requestData.tariCurrency

            exolixRetrofitService.createExchange(
                Exolix.CreateExchangeRequest(
                    coinFrom = fromCurrency.coin,
                    networkFrom = fromCurrency.networkName,
                    coinTo = toCurrency.coin,
                    networkTo = toCurrency.networkName,
                    amount = requestData.amount,
                    withdrawalAddress = toAddress,
                    rateType = requestData.rateType,
                )
            ).also { response ->
                val transaction = exolixRetrofitService.getTransaction(response.id)
                exolixPrefRepository.saveTransaction(transaction)
            }
        }
    }

    /**
     * Get all pending transactions with refreshed details from the API.
     * Each transaction is fetched from the API to get the latest status and details.
     */
    suspend fun getPendingTransactions(): Result<List<Exolix.Transaction>> = switchToIo {
        runCatching {
            val storedTransactions = exolixPrefRepository.transactions
            val refreshedTransactions = mutableListOf<Exolix.Transaction>()

            storedTransactions.forEach { storedTx ->
                val refreshedTx = runCatching {
                    exolixRetrofitService.getTransaction(storedTx.id)
                }.getOrNull()

                if (refreshedTx != null) {
                    exolixPrefRepository.saveTransaction(refreshedTx)
                    refreshedTransactions.add(refreshedTx)
                } else {
                    // If API call fails, keep the stored version
                    refreshedTransactions.add(storedTx)
                }
            }

            refreshedTransactions.sortedByDescending { it.createdAt }
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
    val tariCurrency: CurrencyDto,
    val selectedAddress: String? = null,
    val amount: BigDecimal,
    val rate: Exolix.Rate,
    val rateType: Exolix.RateType,
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