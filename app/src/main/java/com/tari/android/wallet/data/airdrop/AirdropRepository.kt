package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.util.extension.switchToIo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirdropRepository @Inject constructor(
    private val airdropRetrofit: AirdropRetrofitService,
    private val corePrefRepository: CorePrefRepository,
) {

    private val _isAirdropLoggedIn = MutableStateFlow(corePrefRepository.airdropToken != null)
    val isAirdropLoggedIn = _isAirdropLoggedIn.asStateFlow()

    fun saveAirdropToken(token: String) {
        corePrefRepository.airdropToken = token
        _isAirdropLoggedIn.value = true
    }

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

    @Throws(HttpException::class)
    suspend fun getMiningStatus(id: String): Boolean = switchToIo { airdropRetrofit.getMinerStatus(id).mining }

    @Throws(HttpException::class)
    suspend fun getUserDetails(): UserDetailsResponse = switchToIo {
        airdropRetrofit.getUserDetails(token = "Bearer ${corePrefRepository.airdropToken}")
    }

    @Throws(HttpException::class)
    suspend fun getReferralList(): ReferralStatusResponse = switchToIo {
        airdropRetrofit.getReferralStatus(token = "Bearer ${corePrefRepository.airdropToken}")
    }

    companion object {
        private const val MINERS_COUNT_REFRESH_INTERVAL = 60_000L
    }
}