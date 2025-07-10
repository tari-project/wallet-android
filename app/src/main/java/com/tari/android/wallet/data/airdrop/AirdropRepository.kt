package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.util.extension.safeCastTo
import com.tari.android.wallet.util.extension.sha256
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

    suspend fun getMinerStats(): Result<Int> = switchToIo {
        runCatching { airdropRetrofit.getMinerStats().totalMiners }
    }

    suspend fun getMiningStatus(wallet: FFIWallet): Result<Boolean> = switchToIo {
        // any of airdropToken or anonId should be non-null to get mining status
        runCatching {
            airdropRetrofit.getMinerStatus(
                token = "Bearer ${corePrefRepository.airdropToken}",
                viewKeyHashed = wallet.getPrivateViewKey().getByteVector().hex().sha256(),
                anonId = corePrefRepository.airdropAnonId ?: "unset",
            ).mining
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

    fun clear() {
        clearAirdropToken()
        _airdropToken.value = null
    }
}