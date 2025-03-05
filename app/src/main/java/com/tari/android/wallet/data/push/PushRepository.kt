package com.tari.android.wallet.data.push

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.ffi.FFIWallet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushRepository @Inject constructor(
    private val pushRetrofitService: PushRetrofitService,
) {

    suspend fun registerPushToken(
        wallet: FFIWallet,
        fcmToken: String,
        anonId: String? = null,
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
                appId = anonId,
                publicNonce = nonce
            )
        )
    }
}