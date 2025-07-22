package com.tari.android.wallet.data.rwa

import retrofit2.http.GET

interface RwaRetrofitService {

    @GET("/mobile/version")
    suspend fun getMobileVersion(): VersionResponse

    data class VersionResponse(
        val minAndroidVersion: String,
        val recommendedAndroidVersion: String,
    )
}