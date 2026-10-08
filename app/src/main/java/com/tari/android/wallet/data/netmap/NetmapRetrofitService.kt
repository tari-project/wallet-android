package com.tari.android.wallet.data.netmap

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface NetmapRetrofitService {

    @GET("/api/v1/stats")
    suspend fun getStats(): NetmapStatsResponse
}

data class NetmapStatsResponse(
    @SerializedName("confirmed_nodes_24h") val confirmedNodes24h: Int,
)
