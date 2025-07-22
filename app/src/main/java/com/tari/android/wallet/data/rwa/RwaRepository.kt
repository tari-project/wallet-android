package com.tari.android.wallet.data.rwa

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RwaRepository @Inject constructor(
    private val rwaRetrofitService: RwaRetrofitService,
) {

    suspend fun getMobileVersion(): RwaRetrofitService.VersionResponse {
        return rwaRetrofitService.getMobileVersion()
    }
}