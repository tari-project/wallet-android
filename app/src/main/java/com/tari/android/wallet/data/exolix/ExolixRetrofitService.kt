package com.tari.android.wallet.data.exolix

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal

interface ExolixRetrofitService {

    @GET("/api/v2/currencies")
    suspend fun getCurrencies(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("search") search: String? = null,
        @Query("withNetworks") withNetworks: Boolean = true,
    ): Exolix.CurrenciesResponse

    @GET("/api/v2/currencies/{code}/networks")
    suspend fun getCurrencyNetworks(@Path("code") code: String): List<Exolix.Network>

    @GET("/api/v2/currencies/networks")
    suspend fun getAllNetworks(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("search") search: String? = null,
    ): Exolix.NetworksResponse

    @GET("/api/v2/rate")
    suspend fun getRate(
        @Query("coinFrom") coinFrom: String,
        @Query("networkFrom") networkFrom: String? = null,
        @Query("coinTo") coinTo: String,
        @Query("networkTo") networkTo: String? = null,
        @Query("amount") amount: String,
        @Query("withdrawalAmount") withdrawalAmount: String? = null,
        @Query("rateType") rateType: Exolix.RateType = Exolix.RateType.FIXED,
    ): Exolix.Rate

    @GET("/api/v2/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int? = null,
        @Query("size") size: Int? = null,
        @Query("search") search: String? = null,
        @Query("sort") sort: String? = null,
        @Query("order") order: String? = null,
        @Query("dateFrom") dateFrom: String? = null,
        @Query("dateTo") dateTo: String? = null,
        @Query("statuses") statuses: String? = null,
    ): Exolix.TransactionsResponse

    @GET("/api/v2/transactions/{id}")
    suspend fun getTransaction(@Path("id") id: String): Exolix.TransactionResponse

    @POST("/api/v2/transactions")
    @Headers("Content-Type: application/json")
    suspend fun createExchange(@Body request: Exolix.CreateExchangeRequest): Exolix.CreateExchangeResponse
}

object Exolix {

    enum class RateType(val value: String) {
        @SerializedName("fixed")
        FIXED("fixed"),

        @SerializedName("float")
        FLOATING("float");

        override fun toString(): String = value
    }

    enum class TransactionStatus(val value: String) {
        @SerializedName("wait")
        WAIT("wait"),

        @SerializedName("confirmation")
        CONFIRMATION("confirmation"),

        @SerializedName("confirmed")
        CONFIRMED("confirmed"),

        @SerializedName("exchanging")
        EXCHANGING("exchanging"),

        @SerializedName("sending")
        SENDING("sending"),

        @SerializedName("success")
        SUCCESS("success"),

        @SerializedName("overdue")
        OVERDUE("overdue"),

        @SerializedName("refunded")
        REFUNDED("refunded");

        override fun toString(): String = value
    }

    data class CreateExchangeRequest(
        @SerializedName("coinFrom") val coinFrom: String,
        @SerializedName("networkFrom") val networkFrom: String,
        @SerializedName("coinTo") val coinTo: String,
        @SerializedName("networkTo") val networkTo: String,
        @SerializedName("amount") val amount: BigDecimal,
        @SerializedName("withdrawalAmount") val withdrawalAmount: BigDecimal? = null,
        @SerializedName("withdrawalAddress") val withdrawalAddress: String,
        @SerializedName("withdrawalExtraId") val withdrawalExtraId: String? = null,
        @SerializedName("rateType") val rateType: RateType = RateType.FIXED,
        @SerializedName("refundAddress") val refundAddress: String? = null,
        @SerializedName("refundExtraId") val refundExtraId: String? = null,
        @SerializedName("slippage") val slippage: Double? = null,
    )

    data class CurrenciesResponse(
        @SerializedName("data") val data: List<Currency>,
        @SerializedName("count") val count: Int,
    )

    @Parcelize
    data class Currency(
        @SerializedName("code") val code: String,
        @SerializedName("name") val name: String,
        @SerializedName("icon") val icon: String,
        @SerializedName("notes") val notes: String,
        @SerializedName("networks") val networks: List<Network>? = null,
    ) : Parcelable

    @Parcelize
    data class Network(
        @SerializedName("network") val network: String,
        @SerializedName("name") val name: String,
        @SerializedName("shortName") val shortName: String? = null,
        @SerializedName("notes") val notes: String? = null,
        @SerializedName("addressRegex") val addressRegex: String? = null,
        @SerializedName("isDefault") val isDefault: Boolean,
        @SerializedName("blockExplorer") val blockExplorer: String? = null,
        @SerializedName("memoNeeded") val memoNeeded: Boolean,
        @SerializedName("memoName") val memoName: String? = null,
        @SerializedName("memoRegex") val memoRegex: String? = null,
        @SerializedName("decimal") val decimal: Int? = null,
        @SerializedName("precision") val precision: Int,
        @SerializedName("contract") val contract: String? = null,
        @SerializedName("icon") val icon: String? = null,
    ) : Parcelable {
        override fun equals(other: Any?): Boolean {
            return other is Network && other.network == network
        }
    }

    data class NetworksResponse(
        @SerializedName("data") val data: List<Network>,
        @SerializedName("count") val count: Int,
    )

    @Parcelize
    data class Rate(
        @SerializedName("fromAmount") val fromAmount: BigDecimal,
        @SerializedName("toAmount") val toAmount: BigDecimal,
        @SerializedName("rate") val rate: BigDecimal = BigDecimal.ZERO,
        @SerializedName("message") val message: String? = null,
        @SerializedName("minAmount") val minAmount: BigDecimal,
        @SerializedName("withdrawMin") val withdrawMin: BigDecimal = BigDecimal.ZERO,
        @SerializedName("maxAmount") val maxAmount: BigDecimal,
    ) : Parcelable

    data class TransactionsResponse(
        @SerializedName("data") val data: List<Transaction>,
        @SerializedName("count") val count: Int,
    )

    data class Transaction(
        @SerializedName("id") val id: String,
        @SerializedName("amount") val amount: BigDecimal,
        @SerializedName("amountTo") val amountTo: BigDecimal,
        @SerializedName("coinFrom") val coinFrom: CoinInfo,
        @SerializedName("coinTo") val coinTo: CoinInfo,
        @SerializedName("comment") val comment: String? = null,
        @SerializedName("createdAt") val createdAt: String,
        @SerializedName("depositAddress") val depositAddress: String,
        @SerializedName("depositExtraId") val depositExtraId: String? = null,
        @SerializedName("withdrawalAddress") val withdrawalAddress: String,
        @SerializedName("withdrawalExtraId") val withdrawalExtraId: String? = null,
        @SerializedName("hashIn") val hashIn: HashInfo? = null,
        @SerializedName("hashOut") val hashOut: HashInfo? = null,
        @SerializedName("rate") val rate: BigDecimal,
        @SerializedName("rateType") val rateType: RateType,
        @SerializedName("refundAddress") val refundAddress: String? = null,
        @SerializedName("refundExtraId") val refundExtraId: String? = null,
        @SerializedName("status") val status: TransactionStatus,
        @SerializedName("source") val source: String? = null,
    )

    data class TransactionResponse(
        @SerializedName("id") val id: String,
        @SerializedName("amount") val amount: BigDecimal,
        @SerializedName("amountTo") val amountTo: BigDecimal,
        @SerializedName("coinFrom") val coinFrom: CoinInfo,
        @SerializedName("coinTo") val coinTo: CoinInfo,
        @SerializedName("comment") val comment: String? = null,
        @SerializedName("createdAt") val createdAt: String,
        @SerializedName("depositAddress") val depositAddress: String,
        @SerializedName("depositExtraId") val depositExtraId: String? = null,
        @SerializedName("withdrawalAddress") val withdrawalAddress: String,
        @SerializedName("withdrawalExtraId") val withdrawalExtraId: String? = null,
        @SerializedName("hashIn") val hashIn: HashInfo? = null,
        @SerializedName("hashOut") val hashOut: HashInfo? = null,
        @SerializedName("rate") val rate: BigDecimal,
        @SerializedName("rateType") val rateType: RateType,
        @SerializedName("refundAddress") val refundAddress: String? = null,
        @SerializedName("refundExtraId") val refundExtraId: String? = null,
        @SerializedName("status") val status: TransactionStatus,
        @SerializedName("source") val source: String? = null,
    )

    data class CoinInfo(
        @SerializedName("coinCode") val coinCode: String,
        @SerializedName("coinName") val coinName: String,
        @SerializedName("network") val network: String,
        @SerializedName("networkName") val networkName: String,
        @SerializedName("networkShortName") val networkShortName: String? = null,
        @SerializedName("icon") val icon: String,
        @SerializedName("memoName") val memoName: String? = null,
        @SerializedName("contract") val contract: String? = null,
    )

    data class HashInfo(
        @SerializedName("hash") val hash: String? = null,
        @SerializedName("link") val link: String? = null,
    )

    data class CreateExchangeResponse(
        @SerializedName("id") val id: String,
        @SerializedName("amount") val amount: BigDecimal,
        @SerializedName("amountTo") val amountTo: BigDecimal,
        @SerializedName("coinFrom") val coinFrom: CoinInfo,
        @SerializedName("coinTo") val coinTo: CoinInfo,
        @SerializedName("comment") val comment: String? = null,
        @SerializedName("createdAt") val createdAt: String,
        @SerializedName("depositAddress") val depositAddress: String,
        @SerializedName("depositExtraId") val depositExtraId: String? = null,
        @SerializedName("withdrawalAddress") val withdrawalAddress: String,
        @SerializedName("withdrawalExtraId") val withdrawalExtraId: String? = null,
        @SerializedName("hashIn") val hashIn: HashInfo? = null,
        @SerializedName("hashOut") val hashOut: HashInfo? = null,
        @SerializedName("rate") val rate: BigDecimal,
        @SerializedName("rateType") val rateType: RateType,
        @SerializedName("refundAddress") val refundAddress: String? = null,
        @SerializedName("refundExtraId") val refundExtraId: String? = null,
        @SerializedName("status") val status: TransactionStatus,
    )
}