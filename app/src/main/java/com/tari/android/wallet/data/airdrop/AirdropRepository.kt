package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.util.extension.safeCastTo
import com.tari.android.wallet.util.extension.switchToIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    suspend fun getMinerStats(): Result<Int> = switchToIo {
        runCatching { airdropRetrofit.getMinerStats().totalMiners }
    }

    suspend fun getMiningStatus(): Result<Boolean> = switchToIo {
        val airdropAnonId = corePrefRepository.airdropAnonId ?: return@switchToIo Result.failure(IllegalStateException("Airdrop anon ID is null"))
        runCatching {
            airdropRetrofit.getMinerStatus(airdropAnonId).mining
        }
    }

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
}