package com.tari.android.wallet.application.walletManager

import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.TransactionSendStatus
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.Locale
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WalletNotificationManager @Inject constructor(
    private val app: TariWalletApplication,
    private val notificationHelper: NotificationHelper,
    private val notificationService: NotificationService,
) {

    /**
     * Debounce for inbound transaction notification.
     * TODO don't use rx. Replace with coroutines.
     */
    private var txReceivedNotificationDelayedAction: Disposable? = null
    private var inboundTxEventNotificationTxs = mutableListOf<Tx>()
    private val outboundTxIdsToBePushNotified = CopyOnWriteArraySet<OutboundTxNotification>()

    fun postTxNotification(tx: Tx) {
        txReceivedNotificationDelayedAction?.dispose()
        inboundTxEventNotificationTxs.add(tx)
        txReceivedNotificationDelayedAction =
            Observable.timer(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    // if app is backgrounded, display heads-up notification
                    val currentActivity = app.currentActivity
                    if (!app.isInForeground
                        || currentActivity !is HomeActivity
                        || !currentActivity.willNotifyAboutNewTx()
                    ) {
                        notificationHelper.postCustomLayoutTxNotification(inboundTxEventNotificationTxs.last())
                    }
                    inboundTxEventNotificationTxs.clear()
                }
    }

    fun addOutboundTxNotification(txId: TxId, recipientAddress: FFITariWalletAddress) {
        outboundTxIdsToBePushNotified.add(
            OutboundTxNotification(
                txId = txId,
                recipientPublicKeyHex = recipientAddress.notificationHex().lowercase(Locale.ENGLISH),
            )
        )
    }

    fun sendOutboundTxNotification(wallet: FFIWallet, txId: TxId, status: TransactionSendStatus) {
        outboundTxIdsToBePushNotified.firstOrNull { it.txId == txId }?.let {
            outboundTxIdsToBePushNotified.remove(it)
            val senderHex = wallet.getWalletAddress().notificationHex()
            notificationService.notifyRecipient(it.recipientPublicKeyHex, senderHex, wallet::signMessage)
        }
    }

    private data class OutboundTxNotification(val txId: TxId, val recipientPublicKeyHex: String)
}
