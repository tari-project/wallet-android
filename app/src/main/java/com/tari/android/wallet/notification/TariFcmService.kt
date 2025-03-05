package com.tari.android.wallet.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.di.DiContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class TariFcmService : FirebaseMessagingService() {

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var fcmHelper: FcmHelper

    private val logger
        get() = Logger.t(TariFcmService::class.simpleName)

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    init {
        DiContainer.appComponent.inject(this)
    }

    // Messages are not handled here when the app is backgrounded or terminated. Specify Intent for your actions!
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.d("From: ${remoteMessage.from}")
    }

    override fun onNewToken(token: String) {
        logger.d("Refreshed FCM token: $token")

        serviceScope.launch {
            walletManager.doOnWalletRunning { wallet ->
                fcmHelper.registerFcmToken(wallet, token)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceJob.cancel()
    }
}