package com.tari.android.wallet.data.airdrop

import retrofit2.http.GET

interface AirdropRetrofitService {

    @GET("/api/miner/stats")
    suspend fun getMinerStats(): MinerStatsResponse
}

data class MinerStatsResponse(
    val totalMiners: Int,
)