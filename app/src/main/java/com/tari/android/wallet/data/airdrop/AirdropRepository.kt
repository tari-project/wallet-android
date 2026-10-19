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

    private val _airdropToken = MutableStateFlow(corePrefRepository.airdropToken)
    val airdropToken = _airdropToken.asStateFlow()

    fun saveAirdropToken(token: String, refreshToken: String) {
        corePrefRepository.airdropToken = token
        corePrefRepository.airdropRefreshToken = refreshToken
        _airdropToken.value = token
    }

    fun clearAirdropToken() {
        corePrefRepository.airdropToken = null
        corePrefRepository.airdropRefreshToken = null
        _airdropToken.value = null
    }

    fun markLoginPending(now: Long) {
        corePrefRepository.airdropLoginPendingSince = now.toString()
    }

    fun getLoginPendingSince(): Long? = corePrefRepository.airdropLoginPendingSince?.toLongOrNull()

    fun clearLoginPending() {
        corePrefRepository.airdropLoginPendingSince = null
    }

    /**
     * Exchanges a not-yet-saved refresh token for a new token pair and looks up the account it belongs to, without touching the stored
     * session. Returns null if either call fails or the account has no name.
     */
    suspend fun verifyAccount(refreshToken: String): VerifiedAirdropAccount? = switchToIo {
        runCatching {
            val tokens = airdropRetrofit.refreshAuthToken(RefreshTokenRequest(refreshToken))
            val user = airdropRetrofit.getUserDetails(token = "Bearer ${tokens.token}").user
            val name = user.displayName?.takeIf { it.isNotBlank() } ?: user.name
            if (name.isBlank() || tokens.token.isBlank() || tokens.refreshToken.isBlank()) {
                null
            } else {
                VerifiedAirdropAccount(token = tokens.token, refreshToken = tokens.refreshToken, name = name)
            }
        }.getOrNull()
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

    fun clear() {
        clearAirdropToken()
        _airdropToken.value = null
    }
}

data class VerifiedAirdropAccount(
    val token: String,
    val refreshToken: String,
    val name: String,
)
