package com.tari.android.wallet.notification

import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.push.PushRepository
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.ffi.FFIWallet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FcmHelper @Inject constructor(
    private val pushRepository: PushRepository,
    private val corePrefs: CorePrefRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(FcmHelper::class.simpleName)

    fun getFcmTokenAndRegister(wallet: FFIWallet) {
        applicationScope.launch {
            getFcmToken { token ->
                registerFcmToken(wallet, token)
            }
        }
    }

    fun registerFcmToken(wallet: FFIWallet, token: String) {
        applicationScope.launch {
            try {
                pushRepository.registerPushToken(
                    wallet = wallet,
                    fcmToken = token,
                )
            } catch (e: Exception) {
                logger.i("FCM token registration failed for token: $token\n with exception: $e")
            }
        }
    }

    private fun getFcmToken(onTokenReceived: (token: String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (task.isSuccessful) {
                task.result?.let { token ->
                    logger.i("FCM registration token: $token")
                    onTokenReceived(token)
                }
            } else {
                logger.i("Fetching FCM registration token failed", task.exception)
            }
        })
    }
}