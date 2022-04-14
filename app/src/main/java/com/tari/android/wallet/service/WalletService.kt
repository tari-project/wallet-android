/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetFaucetRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetUtxoList
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.orEmpty
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.Tx.Direction.INBOUND
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.WalletServiceLauncher.Companion.startAction
import com.tari.android.wallet.service.WalletServiceLauncher.Companion.stopAction
import com.tari.android.wallet.service.WalletServiceLauncher.Companion.stopAndDeleteAction
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.faucet.TestnetFaucetService
import com.tari.android.wallet.service.faucet.TestnetTariRequestException
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import java.math.BigInteger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Foreground wallet service.
 *
 * @author The Tari Development Team
 */
internal class WalletService : Service(), FFIWalletListener, LifecycleObserver {

    companion object {
        // notification id
        private const val NOTIFICATION_ID = 1
        private const val MESSAGE_PREFIX = "Hello Tari from"

        // key-value storage keys
        object KeyValueStorageKeys {
            const val NETWORK = "SU7FM2O6Q3BU4XVN7HDD"
        }
    }

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var app: TariWalletApplication

    @Inject
    lateinit var testnetFaucetService: TestnetFaucetService

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var baseNodeSharedPrefsRepository: BaseNodeSharedRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var baseNodes: BaseNodes

    @Inject
    lateinit var testnetFaucetRepository: TestnetFaucetRepository

    private lateinit var wallet: FFIWallet

    private var txBroadcastRestarted = false

    /**
     * Pairs of <tx id, recipient public key hex>.
     */
    private val outboundTxIdsToBePushNotified = CopyOnWriteArraySet<Pair<BigInteger, String>>()

    /**
     * Service stub implementation.
     */
    private val serviceImpl = TariWalletServiceImpl()

    /**
     * Registered listeners.
     */
    private var listeners = CopyOnWriteArrayList<TariWalletServiceListener>()

    /**
     * Check for expired txs every 30 minutes.
     */
    private val expirationCheckPeriodMinutes = Minutes.minutes(30)

    /**
     * Switch to low power mode 3 minutes after the app gets backgrounded.
     */
    private val backgroundLowPowerModeSwitchMinutes = Minutes.minutes(3)

    /**
     * Timer to trigger the expiration checks.
     */
    private var txExpirationCheckSubscription: Disposable? = null

    private var lowPowerModeSubscription: Disposable? = null

    private enum class BaseNodeValidationType {
        TXO,
        TX;
    }

    /**
     * Debounce for inbound transaction notification.
     */
    private var txReceivedNotificationDelayedAction: Disposable? = null
    private var inboundTxEventNotificationTxs = mutableListOf<Tx>()


    override fun onCreate() {
        super.onCreate()
        DiContainer.appComponent.inject(this)
    }

    /**
     * Called when a component decides to start or stop the foreground wallet service.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground()
        when (intent.action) {
            startAction -> startService()
            stopAction -> stopService(startId)
            stopAndDeleteAction -> {
                //todo total crutch. Service is auto-creating during the bind func. Need to refactor this first
                DiContainer.appComponent.inject(this)
                stopService(startId)
                deleteWallet()
            }
            else -> throw RuntimeException("Unexpected intent action: ${intent.action}")
        }
        return START_NOT_STICKY
    }


    private fun startService() {
        //todo total crutch. Service is auto-creating during the bind func. Need to refactor this first
        DiContainer.appComponent.inject(this)
        // start wallet manager on a separate thead & listen to events
        EventBus.walletState.subscribe(this, this::onWalletStateChanged)
        Thread {
            walletManager.start()
        }.start()
        Logger.d("Wallet service started.")
    }

    private fun startForeground() {
        // start service & post foreground service notification
        val notification = notificationHelper.buildForegroundServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopService(startId: Int) {
        // stop service
        stopForeground(true)
        stopSelfResult(startId)
        // stop wallet manager on a separate thead & unsubscribe from events
        EventBus.walletState.unsubscribe(this)
        ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
        GlobalScope.launch { backupManager.turnOffAll(deleteExistingBackups = false) }
        Thread {
            walletManager.stop()
        }.start()
    }

    private fun deleteWallet() {
        WalletUtil.clearWalletFiles(walletConfig.getWalletFilesDirPath())
        sharedPrefsWrapper.clear()
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.Started) {
            wallet = FFIWallet.instance!!
            wallet.listener = this
            EventBus.walletState.unsubscribe(this)
            scheduleExpirationCheck()
            backupManager.initialize()
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                ProcessLifecycleOwner.get().lifecycle.addObserver(this)
            }
            EventBus.walletState.post(WalletState.Running)
        }
    }

    private fun scheduleExpirationCheck() {
        txExpirationCheckSubscription =
            Observable
                .timer(expirationCheckPeriodMinutes.minutes.toLong(), TimeUnit.MINUTES)
                .repeat()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    cancelExpiredPendingInboundTxs()
                    cancelExpiredPendingOutboundTxs()
                }
    }

    /**
     * Bound service.
     */
    override fun onBind(intent: Intent?): IBinder {
        Logger.d("Wallet service bound.")
        return serviceImpl
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.d("Wallet service unbound.")
        return super.onUnbind(intent)
    }

    /**
     * A broadcast is made on destroy to get the service running again.
     */
    override fun onDestroy() {
        Logger.d("Wallet service destroyed.")
        txExpirationCheckSubscription?.dispose()
        sendBroadcast(
            Intent(this, ServiceRestartBroadcastReceiver::class.java)
        )
        super.onDestroy()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackgrounded() {
        // schedule low power mode
        lowPowerModeSubscription =
            Observable
                .timer(backgroundLowPowerModeSwitchMinutes.minutes.toLong(), TimeUnit.MINUTES)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    switchToLowPowerMode()
                }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForegrounded() {
        switchToNormalPowerMode()
    }

    private fun switchToNormalPowerMode() {
        Logger.d("Switch to normal power mode.")
        lowPowerModeSubscription?.dispose()
        try {
            wallet.setPowerModeNormal()
        } catch (e: FFIException) { // silent fail
            Logger.e("FFI error #${e.error?.code} while switching to normal power mode.")
        }
    }

    private fun switchToLowPowerMode() {
        Logger.d("Switch to low power mode.")
        try {
            wallet.setPowerModeLow()
        } catch (e: FFIException) { // silent fail
            Logger.e("FFI error #${e.error?.code} while switching to low power mode.")
        }
    }

    override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
        Logger.d("Tx received: $pendingInboundTx")
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        Logger.d("Received TX after contact update: $pendingInboundTx")
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxReceived(pendingInboundTx))
        // manage notifications
        postTxNotification(pendingInboundTx)
        listeners.forEach { it.onTxReceived(pendingInboundTx) }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) {
        Logger.d("Tx ${pendingOutboundTx.id} reply received.")
        pendingOutboundTx.user = getUserByPublicKey(pendingOutboundTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxReplyReceived(pendingOutboundTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxReplyReceived(pendingOutboundTx)
        }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTxFinalized(pendingInboundTx: PendingInboundTx) {
        Logger.d("Tx ${pendingInboundTx.id} finalized.")
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxFinalized(pendingInboundTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxFinalized(pendingInboundTx)
        }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) {
        Logger.d("Inbound tx ${pendingInboundTx.id} broadcast.")
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.InboundTxBroadcast(pendingInboundTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onInboundTxBroadcast(pendingInboundTx)
        }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) {
        Logger.d("Outbound tx ${pendingOutboundTx.id} broadcast.")
        pendingOutboundTx.user = getUserByPublicKey(pendingOutboundTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.OutboundTxBroadcast(pendingOutboundTx))
        // notify external listeners
        listeners.iterator().forEach { it.onOutboundTxBroadcast(pendingOutboundTx) }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTxMined(completedTx: CompletedTx) {
        Logger.d("Tx ${completedTx.id} mined.")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxMined(completedTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxMined(completedTx)
        }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
        Logger.d("Tx ${completedTx.id} mined, yet unconfirmed. Confirmation count: $confirmationCount")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxMinedUnconfirmed(completedTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxMinedUnconfirmed(completedTx, confirmationCount)
        }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTxFauxConfirmed(completedTx: CompletedTx) {
        Logger.d("Tx faux ${completedTx.id} confirmed.")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxFauxConfirmed(completedTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFauxConfirmed(completedTx) }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
        Logger.d("Tx faux ${completedTx.id} yet unconfirmed. Confirmation count: $confirmationCount")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Transaction.TxFauxMinedUnconfirmed(completedTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFauxUnconfirmed(completedTx, confirmationCount) }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onDirectSendResult(txId: BigInteger, success: Boolean) {
        Logger.d("Tx $txId direct send completed. Success: $success")
        // post event to bus
        EventBus.post(Event.Transaction.DirectSendResult(TxId(txId), success))
        if (success) {
            outboundTxIdsToBePushNotified.firstOrNull { it.first == txId }?.let {
                outboundTxIdsToBePushNotified.remove(it)
                sendPushNotificationToTxRecipient(it.second)
            }
            // schedule a backup
            backupManager.scheduleBackupAll(resetRetryCount = true)
        }
        // notify external listeners
        listeners.iterator().forEach {
            it.onDirectSendResult(TxId(txId), success)
        }
    }

    override fun onStoreAndForwardSendResult(txId: BigInteger, success: Boolean) {
        Logger.d("Tx $txId store and forward send completed. Success: $success")
        // post event to bus
        EventBus.post(Event.Transaction.StoreAndForwardSendResult(TxId(txId), success))
        if (success) {
            outboundTxIdsToBePushNotified.firstOrNull { it.first == txId }?.let {
                outboundTxIdsToBePushNotified.remove(it)
                sendPushNotificationToTxRecipient(it.second)
            }
            // schedule a backup
            backupManager.scheduleBackupAll(resetRetryCount = true)
        }
        // notify external listeners
        listeners.iterator().forEach {
            it.onStoreAndForwardSendResult(TxId(txId), success)
        }
    }

    override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) {
        Logger.d("Tx cancelled: $cancelledTx")
        cancelledTx.user = getUserByPublicKey(cancelledTx.user.publicKey)
        // post event to bus
        EventBus.post(Event.Transaction.TxCancelled(cancelledTx))
        val currentActivity = app.currentActivity
        if (cancelledTx.direction == INBOUND && !(app.isInForeground && currentActivity is HomeActivity && currentActivity.willNotifyAboutNewTx())
        ) {
            Logger.i("Posting cancellation notification")
            notificationHelper.postTxCanceledNotification(cancelledTx)
        }
        // notify external listeners
        listeners.iterator().forEach { listener -> listener.onTxCancelled(cancelledTx) }
        // schedule a backup
        backupManager.scheduleBackupAll(resetRetryCount = true)
    }

    override fun onTXOValidationComplete(responseId: BigInteger, isSuccess: Boolean) {
        Logger.i("onTXOValidationComplete responseId: $responseId, isSuccess: $isSuccess")
    }

    override fun onTxValidationComplete(responseId: BigInteger, isSuccess: Boolean) {
        if (!txBroadcastRestarted && isSuccess) {
            try {
                wallet.restartTxBroadcast()
                txBroadcastRestarted = true
                Logger.i("Transaction broadcast restarted.")
            } catch (e: Exception) {
                Logger.e("Error while restarting tx broadcast: " + e.message)
            }
        }
    }

    override fun onConnectivityStatus(status: Int) {
        when (status) {
            1 -> {
                baseNodeSharedPrefsRepository.baseNodeLastSyncResult = true
                EventBus.baseNodeState.post(BaseNodeState.Online)
                listeners.iterator().forEach { it.onBaseNodeSyncComplete(true) }
            }
            2 -> {
                baseNodeSharedPrefsRepository.baseNodeLastSyncResult = false
                val currentBaseNode = baseNodeSharedPrefsRepository.currentBaseNode
                if (currentBaseNode == null || !currentBaseNode.isCustom) {
                    baseNodes.setNextBaseNode()
                }
                EventBus.baseNodeState.post(BaseNodeState.Offline)
                listeners.iterator().forEach { it.onBaseNodeSyncComplete(false) }
            }
        }
    }

    override fun onWalletRestoration(result: WalletRestorationResult) {
        EventBus.walletRestorationState.post(result)
    }

    /**
     * Cancels expired pending inbound transactions.
     * Expiration period is defined by Constants.Wallet.pendingTxExpirationPeriodHours
     */
    private fun cancelExpiredPendingInboundTxs() {
        val pendingInboundTxs = wallet.getPendingInboundTxs()
        val pendingInboundTxsLength = pendingInboundTxs.getLength()
        val now = DateTime.now().toLocalDateTime()
        for (i in 0 until pendingInboundTxsLength) {
            val tx = pendingInboundTxs.getAt(i)
            val txDate = DateTime(tx.getTimestamp().toLong() * 1000L).toLocalDateTime()
            val hoursPassed = Hours.hoursBetween(txDate, now).hours
            if (hoursPassed >= Constants.Wallet.pendingTxExpirationPeriodHours) {
                val success = wallet.cancelPendingTx(tx.getId())
                Logger.d("Expired pending inbound tx ${tx.getId()}. Success: $success.")
            }
            tx.destroy()
        }
        pendingInboundTxs.destroy()
    }

    /**
     * Cancels expired pending outbound transactions.
     * Expiration period is defined by Constants.Wallet.pendingTxExpirationPeriodHours
     */
    private fun cancelExpiredPendingOutboundTxs() {
        val pendingOutboundTxs = wallet.getPendingOutboundTxs()
        val pendingOutboundTxsLength = wallet.getPendingOutboundTxs().getLength()
        val now = DateTime.now().toLocalDateTime()
        for (i in 0 until pendingOutboundTxsLength) {
            val tx = pendingOutboundTxs.getAt(i)
            val txDate = DateTime(tx.getTimestamp().toLong() * 1000L).toLocalDateTime()
            val hoursPassed = Hours.hoursBetween(txDate, now).hours
            if (hoursPassed >= Constants.Wallet.pendingTxExpirationPeriodHours) {
                val success = wallet.cancelPendingTx(tx.getId())
                Logger.d("Expired pending outbound tx ${tx.getId()}. Success: $success")
            }
            tx.destroy()
        }

        pendingOutboundTxs.destroy()
    }

    private fun postTxNotification(tx: Tx) {
        txReceivedNotificationDelayedAction?.dispose()
        inboundTxEventNotificationTxs.add(tx)
        txReceivedNotificationDelayedAction =
            Observable
                .timer(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    // if app is backgrounded, display heads-up notification
                    val currentActivity = app.currentActivity
                    if (!app.isInForeground
                        || currentActivity !is HomeActivity
                        || !currentActivity.willNotifyAboutNewTx()
                    ) {
                        notificationHelper.postCustomLayoutTxNotification(
                            inboundTxEventNotificationTxs.last()
                        )
                    }
                    inboundTxEventNotificationTxs.clear()
                }
    }

    private fun sendPushNotificationToTxRecipient(recipientPublicKeyHex: String) {
        // the push notification server accepts lower-case hex strings as of now
        val fromPublicKeyHex = wallet.getPublicKey().toString().lowercase(Locale.ENGLISH)
        Logger.d(
            "Will send push notification to recipient %s from %s.",
            recipientPublicKeyHex,
            fromPublicKeyHex
        )
        notificationService.notifyRecipient(
            recipientPublicKeyHex,
            fromPublicKeyHex,
            wallet::signMessage,
            onSuccess = { Logger.i("Push notification successfully sent to recipient.") },
            onFailure = { Logger.e(it, "Push notification failed with exception.") }
        )
    }

    private fun getUserByPublicKey(key: PublicKey): User {
        val contactsFFI = wallet.getContacts()
        for (i in 0 until contactsFFI.getLength()) {
            val contactFFI = contactsFFI.getAt(i)
            val publicKeyFFI = contactFFI.getPublicKey()
            val hex = publicKeyFFI.toString()
            val contact =
                if (hex == key.hexString) Contact(key, contactFFI.getAlias())
                else null
            publicKeyFFI.destroy()
            contactFFI.destroy()
            if (contact != null) {
                contactsFFI.destroy()
                return contact
            }
        }
        // destroy native collection
        contactsFFI.destroy()
        return User(key)
    }

    /**
     * Implementation of the AIDL service definition.
     */
    inner class TariWalletServiceImpl : TariWalletService.Stub() {

        private var _cachedContacts: List<Contact>? = null
        private val cachedContacts: List<Contact>
            @Synchronized get() {
                _cachedContacts?.let {
                    return it
                }
                val contactsFFI = wallet.getContacts()
                val contacts = mutableListOf<Contact>()
                for (i in 0 until contactsFFI.getLength()) {
                    val contactFFI = contactsFFI.getAt(i)
                    val publicKeyFFI = contactFFI.getPublicKey()
                    contacts.add(
                        Contact(
                            publicKeyFromFFI(publicKeyFFI),
                            contactFFI.getAlias()
                        )
                    )
                    // destroy native objects
                    publicKeyFFI.destroy()
                    contactFFI.destroy()
                }
                // destroy native collection
                contactsFFI.destroy()
                return contacts.sortedWith(compareBy { it.alias }).also {
                    _cachedContacts = it
                }
            }


        private fun mapThrowableIntoError(walletError: WalletError, throwable: Throwable) {
            if (throwable is FFIException) {
                if (throwable.error != null) {
                    walletError.code = throwable.error.code
                    return
                }
            }
            walletError.code = WalletError.UnknownError.code
        }

        private fun <T> executeWithMapping(walletError: WalletError, onError: (Throwable) -> (Unit) = {}, action: () -> T?): T? {
            return try {
                action()
            } catch (throwable: Throwable) {
                onError(throwable)
                mapThrowableIntoError(walletError, throwable)
                null
            }
        }

        private fun getContactByPublicKeyHexString(hexString: String): Contact? = cachedContacts.firstOrNull { it.publicKey.hexString == hexString }

        override fun registerListener(listener: TariWalletServiceListener): Boolean {
            listeners.add(listener)
            listener.asBinder().linkToDeath({
                val removeSuccessful = listeners.remove(listener)
                Logger.i("Listener died. Remove successful: $removeSuccessful.")
            }, 0)
            return true
        }

        override fun unregisterListener(listener: TariWalletServiceListener): Boolean = listeners.remove(listener)

        override fun getPublicKeyHexString(error: WalletError): String? = executeWithMapping(error) { wallet.getPublicKey().toString() }

        override fun getBalanceInfo(error: WalletError): BalanceInfo? = executeWithMapping(error) { wallet.getBalance() }

        override fun estimateTxFee(amount: MicroTari, error: WalletError): MicroTari? = executeWithMapping(error) {
            val defaultKernelCount = BigInteger("1")
            val defaultOutputCount = BigInteger("2")
            MicroTari(
                wallet.estimateTxFee(
                    amount.value,
                    Constants.Wallet.defaultFeePerGram.value,
                    defaultKernelCount,
                    defaultOutputCount
                )
            )
        }

        /**
         * Get all contacts.
         */
        override fun getContacts(error: WalletError): List<Contact>? = executeWithMapping(error) { cachedContacts }

        /**
         * Get all completed transactions.
         * Client-facing function.
         */
        override fun getCompletedTxs(error: WalletError): List<CompletedTx>? = executeWithMapping(error) {
            val completedTxsFFI = wallet.getCompletedTxs()
            (0 until completedTxsFFI.getLength())
                .map { CompletedTx(completedTxsFFI.getAt(it)) }
                .also { completedTxsFFI.destroy() }
        }

        override fun getCancelledTxs(error: WalletError): List<CancelledTx>? = executeWithMapping(error) {
            val canceledTxsFFI = wallet.getCancelledTxs()
            (0 until canceledTxsFFI.getLength())
                .map { CancelledTx(canceledTxsFFI.getAt(it)) }
                .also { canceledTxsFFI.destroy() }
        }

        /**
         * Get completed transaction by id.
         * Client-facing function.
         */
        override fun getCancelledTxById(id: TxId, error: WalletError): CancelledTx? = executeWithMapping(error) {
            CancelledTx(wallet.getCancelledTxById(id.value))
        }

        /**
         * Get completed transaction by id.
         * Client-facing function.
         */
        override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? = executeWithMapping(error) {
            CompletedTx(wallet.getCompletedTxById(id.value))
        }

        /**
         * Get all pending inbound transactions.
         * Client-facing function.
         */
        override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? = executeWithMapping(error) {
            val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
            (0 until pendingInboundTxsFFI.getLength())
                .map { PendingInboundTx(pendingInboundTxsFFI.getAt(it)) }
                .also { pendingInboundTxsFFI.destroy() }
        }

        /**
         * Get pending inbound transaction by id.
         * Client-facing function.
         */
        override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? = executeWithMapping(error) {
            PendingInboundTx(wallet.getPendingInboundTxById(id.value))
        }

        /**
         * Get all pending outbound transactions.
         * Client-facing function.
         */
        override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? = executeWithMapping(error) {
            val pendingOutboundTxsFFI = wallet.getPendingOutboundTxs()
            (0 until pendingOutboundTxsFFI.getLength())
                .map { PendingOutboundTx(pendingOutboundTxsFFI.getAt(it)) }
                .also { pendingOutboundTxsFFI.destroy() }
        }

        /**
         * Get pending outbound transaction by id.
         * Client-facing function.
         */
        override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? = executeWithMapping(error) {
            PendingOutboundTx(wallet.getPendingOutboundTxById(id.value))
        }

        override fun cancelPendingTx(id: TxId, error: WalletError): Boolean = executeWithMapping(error) {
            wallet.cancelPendingTx(id.value)
        } ?: false

        override fun addBaseNodePeer(baseNodePublicKey: String, baseNodeAddress: String, error: WalletError): Boolean = executeWithMapping(error) {
            val publicKeyFFI = FFIPublicKey(HexString(baseNodePublicKey))
            val result = wallet.addBaseNodePeer(publicKeyFFI, baseNodeAddress)
            publicKeyFFI.destroy()
            result
        } ?: false

        override fun startBaseNodeSync(error: WalletError): Boolean = executeWithMapping(error, {
            Logger.e("Base node validation error: $it")
            baseNodeSharedPrefsRepository.baseNodeLastSyncResult = false
        }) {
            baseNodeSharedPrefsRepository.baseNodeLastSyncResult = null
            EventBus.baseNodeState.post(BaseNodeState.SyncStarted)
            true
        } ?: false


        override fun sendTari(
            user: User,
            amount: MicroTari,
            feePerGram: MicroTari,
            message: String,
            isOneSidePayment: Boolean,
            error: WalletError
        ): TxId? = executeWithMapping(error) {
            val recipientPublicKeyHex = user.publicKey.hexString
            val publicKeyFFI = FFIPublicKey(HexString(recipientPublicKeyHex))
            val txId = wallet.sendTx(publicKeyFFI, amount.value, feePerGram.value, message, isOneSidePayment)
            publicKeyFFI.destroy()
            outboundTxIdsToBePushNotified.add(Pair(txId, recipientPublicKeyHex.lowercase(Locale.ENGLISH)))
            TxId(txId)
        }

        // region FFI to model extraction functions
        private fun publicKeyFromFFI(publicKeyFFI: FFIPublicKey): PublicKey {
            return PublicKey(publicKeyFFI.toString(), publicKeyFFI.getEmojiId())
        }

        override fun requestTestnetTari(error: WalletError) {
            // avoid multiple faucet requests
            if (testnetFaucetRepository.faucetTestnetTariRequestCompleted) return
            // get public key
            val publicKeyHexString = getPublicKeyHexString(error)
            if (error.code != WalletError.NoError.code || publicKeyHexString == null) {
                notifyTestnetTariRequestFailed("Service error.")
                return
            }

            val message = "$MESSAGE_PREFIX $publicKeyHexString"
            val signing = wallet.signMessage(message).split("|")
            val signature = signing[0]
            val nonce = signing[1]

            testnetFaucetService.requestMaxTestnetTari(
                publicKeyHexString,
                signature,
                nonce,
                { result ->
                    Logger.i("requestMaxTestnetTari success")
                    val senderPublicKeyFFI = FFIPublicKey(HexString(result.walletId))
                    // add contact
                    FFIContact("TariBot", senderPublicKeyFFI).also {
                        wallet.addUpdateContact(it)
                        it.destroy()
                    }
                    senderPublicKeyFFI.destroy()
                    // update the keys with sender public key hex
                    result.keys.forEach { key -> key.senderPublicKeyHex = result.walletId }
                    // store the UTXO keys
                    testnetFaucetRepository.testnetTariUTXOKeyList = TestnetUtxoList(result.keys)

                    // post event to bus for the internal listeners
                    EventBus.post(Event.Testnet.TestnetTariRequestSuccessful())
                    // notify external listeners
                    listeners.iterator().forEach { it.onTestnetTariRequestSuccess() }
                },
                {
                    Logger.i("requestMaxTestnetTari error ${it.message}")
                    error.code = WalletError.UnknownError.code
                    val errorMessage = string(R.string.wallet_service_error_testnet_tari_request) + " " + it.message
                    // todo maybe needed
//                    error.message = errorMessage
                    if (it is TestnetTariRequestException) {
                        notifyTestnetTariRequestFailed(errorMessage)
                    } else {
                        notifyTestnetTariRequestFailed(string(R.string.wallet_service_error_no_internet_connection))
                    }
                }
            )
        }

        override fun importTestnetUTXO(txMessage: String, error: WalletError): CompletedTx? {
            val keys = testnetFaucetRepository.testnetTariUTXOKeyList.orEmpty()
            if (keys.isEmpty()) return null

            return runCatching {
                val firstUTXOKey = keys.first()
                val senderPublicKeyFFI = FFIPublicKey(HexString(firstUTXOKey.senderPublicKeyHex!!))
                val privateKey = FFIPrivateKey(HexString(firstUTXOKey.key))
                val scriptPrivateKey = FFIPrivateKey(HexString(firstUTXOKey.key))
                val amount = BigInteger(firstUTXOKey.value)
                val senderPublicKey = FFIPublicKey(HexString(firstUTXOKey.output.senderOffsetPublicKey))
                val signature = FFITariCommitmentSignature(
                    FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.public_nonce)),
                    FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.u)),
                    FFIByteVector(HexString(firstUTXOKey.output.metadataSignature.v))
                )
                val txId = wallet.importUTXO(
                    amount,
                    txMessage,
                    privateKey,
                    senderPublicKeyFFI,
                    signature,
                    senderPublicKey,
                    scriptPrivateKey
                )
                privateKey.destroy()
                senderPublicKeyFFI.destroy()
                signature.destroy()
                // remove the used key
                keys.remove(firstUTXOKey)
                testnetFaucetRepository.testnetTariUTXOKeyList = keys
                // get transaction and post notification
                val tx = getCompletedTxById(TxId(txId), error)
                if (error != WalletError.NoError || tx == null) return null

                postTxNotification(tx)
                tx
            }.getOrNull()
        }

        override fun removeContact(contact: Contact, error: WalletError): Boolean = executeWithMapping(error) {
            val contactsFFI = wallet.getContacts()
            for (i in 0 until contactsFFI.getLength()) {
                val contactFFI = contactsFFI.getAt(i)
                val publicKeyFFI = contactFFI.getPublicKey()
                if (publicKeyFFI.toString() == contact.publicKey.hexString) {
                    return@executeWithMapping wallet.removeContact(contactFFI).also {
                        publicKeyFFI.destroy()
                        contactFFI.destroy()
                        contactsFFI.destroy()
                        _cachedContacts = null
                    }
                }
                publicKeyFFI.destroy()
                contactFFI.destroy()
            }
            contactsFFI.destroy()
            false
        } ?: false

        private fun notifyTestnetTariRequestFailed(error: String) {
            // post event to bus for the internal listeners
            EventBus.post(Event.Testnet.TestnetTariRequestError(error))
            // notify external listeners
            listeners.iterator().forEach { listener -> listener.onTestnetTariRequestError(error) }
        }

        override fun updateContactAlias(publicKey: PublicKey, alias: String, error: WalletError): Boolean = executeWithMapping(error) {
            val publicKeyFFI = FFIPublicKey(HexString(publicKey.hexString))
            val contact = FFIContact(alias, publicKeyFFI)
            wallet.addUpdateContact(contact).also {
                publicKeyFFI.destroy()
                contact.destroy()
                _cachedContacts = null
            }
        } ?: false

        /**
         * @return public key constructed from input emoji id. Null if the emoji id is invalid
         * or it does not correspond to a public key.
         */
        override fun getPublicKeyFromEmojiId(emojiId: String?): PublicKey? = runCatching {
            FFIPublicKey(emojiId.orEmpty()).run { publicKeyFromFFI(this).also { destroy() } }
        }.getOrNull()

        /**
         * @return public key constructed from input public key hex string id. Null if the emoji id
         * is invalid or it does not correspond to a public key.
         */
        override fun getPublicKeyFromHexString(publicKeyHex: String?): PublicKey? = runCatching {
            FFIPublicKey(HexString(publicKeyHex ?: "")).run { publicKeyFromFFI(this).also { destroy() } }
        }.getOrNull()

        override fun setKeyValue(key: String, value: String, error: WalletError): Boolean = executeWithMapping(error) {
            wallet.setKeyValue(key, value)
        } ?: false

        override fun getKeyValue(key: String, error: WalletError): String? = executeWithMapping(error) { wallet.getKeyValue(key) }

        override fun removeKeyValue(key: String, error: WalletError): Boolean = executeWithMapping(error) {
            wallet.removeKeyValue(key)
        } ?: false

        override fun getRequiredConfirmationCount(error: WalletError): Long = executeWithMapping(error) {
            wallet.getRequiredConfirmationCount().toLong()
        } ?: 0

        override fun setRequiredConfirmationCount(number: Long, error: WalletError) {
            executeWithMapping(error) { wallet.setRequiredConfirmationCount(BigInteger.valueOf(number)) }
        }

        override fun getSeedWords(error: WalletError): List<String>? = executeWithMapping(error) {
            val seedWordsFFI = wallet.getSeedWords()
            (0 until seedWordsFFI.getLength())
                .map { seedWordsFFI.getAt(it) }
                .also { seedWordsFFI.destroy() }
        }

        // endregion
    }
}


