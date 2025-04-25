package com.tari.android.wallet.data.push

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.ffi.FFIWallet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRepository @Inject constructor(
    private val pushRetrofitService: PushRetrofitService,
    private val corePrefs: CorePrefRepository,
    private val airdropRepository: AirdropRepository,
) {

    suspend fun registerPushToken(
        wallet: FFIWallet,
        fcmToken: String,
    ) {
        val apiKey = BuildConfig.NOTIFICATIONS_API_KEY
        val publicKey = wallet.getWalletAddress().notificationHex()
        val signing = wallet.signMessage(apiKey + publicKey + fcmToken)
        val signature = signing.split("|")[0]
        val nonce = signing.split("|")[1]

        pushRetrofitService.register(
            publicKey = publicKey,
            body = PushRegisterRequestBody(
                token = fcmToken,
                signature = signature,
                appId = corePrefs.airdropAnonId,
                userId = airdropRepository.getUserDetails().getOrNull()?.user?.id,
                publicNonce = nonce
            )
        )
    }
}