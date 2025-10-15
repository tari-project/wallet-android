package com.tari.android.wallet.data.exolix

import com.tari.android.wallet.util.extension.switchToIo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExolixRepository @Inject constructor(
    private val exolixRetrofitService: ExolixRetrofitService,
) {

    // Common methods
    suspend fun getCurrencies(
        page: Int? = null,
        size: Int? = null,
        search: String? = null,
        withNetworks: Boolean? = null
    ): Result<Exolix.CurrenciesResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.getCurrencies(page, size, search, withNetworks)
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
        rateType: String = "fixed"
    ): Result<Exolix.RateResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.getRate(
                coinFrom, networkFrom, coinTo, networkTo,
                amount, withdrawalAmount, rateType
            )
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
            exolixRetrofitService.getTransactions(
                page, size, search, sort, order,
                dateFrom, dateTo, statuses
            )
        }
    }

    // Exchange methods
    suspend fun getTransaction(id: String): Result<Exolix.TransactionResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.getTransaction(id)
        }
    }

    suspend fun createExchange(request: Exolix.CreateExchangeRequest): Result<Exolix.CreateExchangeResponse> = switchToIo {
        runCatching {
            exolixRetrofitService.createExchange(request)
        }
    }
}