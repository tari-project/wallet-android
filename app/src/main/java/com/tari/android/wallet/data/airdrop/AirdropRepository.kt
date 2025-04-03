package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.util.extension.safeCastTo
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

    fun saveAirdropToken(token: String, refreshToken: String) {
        corePrefRepository.airdropToken = token
        corePrefRepository.airdropRefreshToken = refreshToken
        _isAirdropLoggedIn.value = true
    }

    fun clearAirdropToken() {
        corePrefRepository.airdropToken = null
        corePrefRepository.airdropRefreshToken = null
        _isAirdropLoggedIn.value = false
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

    suspend fun getUserDetails(): Result<UserDetailsResponse> = switchToIo {
        val result = runCatching {
            airdropRetrofit.getUserDetails(token = "Bearer ${corePrefRepository.airdropToken}")
        }
        if (result.is401()) {
            val newTokens = runCatching {
                airdropRetrofit.refreshAuthToken(RefreshTokenRequest(corePrefRepository.airdropRefreshToken.orEmpty()))
            }.getOrNull()

            if (newTokens != null) {
                saveAirdropToken(newTokens.token, newTokens.refreshToken)
                return@switchToIo runCatching { airdropRetrofit.getUserDetails(token = "Bearer ${corePrefRepository.airdropToken}") }
            } else {
                clearAirdropToken()
            }
        }
        return@switchToIo result
    }

    suspend fun getReferralList(): Result<ReferralStatusResponse> = switchToIo {
        runCatching {
            airdropRetrofit.getReferralStatus(token = "Bearer ${corePrefRepository.airdropToken}")
        }
    }

    private fun Result<Any>.is401() = this.exceptionOrNull()?.safeCastTo<HttpException>()?.code() == 401

    companion object {
        private const val MINERS_COUNT_REFRESH_INTERVAL = 60_000L
    }
}