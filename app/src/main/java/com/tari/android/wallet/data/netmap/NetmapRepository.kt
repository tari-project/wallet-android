package com.tari.android.wallet.data.netmap

import com.tari.android.wallet.util.extension.switchToIo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetmapRepository @Inject constructor(
    private val netmapRetrofit: NetmapRetrofitService,
) {

    suspend fun getActiveNodesCount(): Result<Int> = switchToIo {
        runCatching { netmapRetrofit.getStats().confirmedNodes24h }
    }
}
