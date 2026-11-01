package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.application.deeplinks.AirdropLoginDeeplinkPolicy
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
     * session. Returns null if either call fails or the account has no displayable name.
     */
    suspend fun verifyAccount(refreshToken: String): VerifiedAirdropAccount? = switchToIo {
        if (refreshToken.isBlank()) return@switchToIo null
        runCatching {
            val tokens = airdropRetrofit.refreshAuthToken(RefreshTokenRequest(refreshToken))
            val user = airdropRetrofit.getUserDetails(token = "Bearer ${tokens.token}").user
            val name = AirdropLoginDeeplinkPolicy.sanitizeAccountName(user.displayName.orEmpty())
                ?: AirdropLoginDeeplinkPolicy.sanitizeAccountName(user.name)
            if (name == null || tokens.token.isBlank() || tokens.refreshToken.isBlank()) {
                null
            } else {
                VerifiedAirdropAccount(token = tokens.token, refreshToken = tokens.refreshToken, name = name)
            }
        }.getOrNull()
    }

    suspend fun getUserDetails(): Result<UserDetailsResponse> = switchToIo {
        withTokenRefresh { token ->
            airdropRetrofit.getUserDetails(token = "Bearer $token")
        }
    }

    suspend fun getReferralList(): Result<ReferralStatusResponse> = switchToIo {
        withTokenRefresh { token ->
            airdropRetrofit.getReferralStatus(token = "Bearer $token")
        }
    }

    private suspend fun <T : Any> withTokenRefresh(block: suspend (token: String?) -> T): Result<T> {
        val result = runCatching {
            block(corePrefRepository.airdropToken)
        }
        if (result.is401()) {
            val refreshToken = corePrefRepository.airdropRefreshToken.orEmpty()
            val newTokens = if (refreshToken.isNotBlank()) {
                runCatching {
                    airdropRetrofit.refreshAuthToken(RefreshTokenRequest(refreshToken))
                }.getOrNull()
            } else {
                null
            }

            if (newTokens != null && newTokens.token.isNotBlank() && newTokens.refreshToken.isNotBlank()) {
                saveAirdropToken(newTokens.token, newTokens.refreshToken)
                return runCatching { block(newTokens.token) }
            } else {
                clearAirdropToken()
            }
        }
        return result
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
