package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.util.extension.switchToIo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.Throws

@Singleton
class AirdropRepository @Inject constructor(
    private val airdropRetrofit: AirdropRetrofitService,
) {

    @Throws(HttpException::class)
    suspend fun getMinerStats(): Int = switchToIo { airdropRetrofit.getMinerStats().totalMiners }

    fun getMinerStatsFlow(): Flow<Int> = flow<Int> {
        while (true) {
            try {
                emit(getMinerStats())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(MINERS_COUNT_REFRESH_INTERVAL)
        }
    }

    companion object {
        private const val MINERS_COUNT_REFRESH_INTERVAL = 60_000L
    }
}