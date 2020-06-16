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

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.Tx.Direction.INBOUND
import com.tari.android.wallet.model.Tx.Direction.OUTBOUND
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.faucet.TestnetFaucetService
import com.tari.android.wallet.service.faucet.TestnetTariRequestException
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.Minutes
import java.lang.StringBuilder
import java.math.BigInteger
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList

/**
 * Foreground wallet service.
 *
 * @author The Tari Development Team
 */
internal class WalletService : Service(), FFIWalletListenerAdapter, LifecycleObserver {

    companion object {
        // notification id
        private const val NOTIFICATION_ID = 1
        private const val MESSAGE_PREFIX = "Hello Tari from"
    }

    @Inject
    lateinit var app: TariWalletApplication

    @Inject
    lateinit var testnetFaucetService: TestnetFaucetService

    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var walletManager: WalletManager

    private lateinit var wallet: FFIWallet

    /**
     * Pairs of <tx id, recipient public key hex>.
     */
    private val outboundTxIdsToBePushNotified = mutableSetOf<Pair<BigInteger, String>>()

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
    private val txExpirationCheckSubscription =
        Observable
            .timer(expirationCheckPeriodMinutes.minutes.toLong(), TimeUnit.MINUTES)
            .repeat()
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe {
                cancelExpiredPendingInboundTxs()
                cancelExpiredPendingOutboundTxs()
            }

    private var lowPowerModeSubscription: Disposable? = null

    override fun onCreate() {
        super.onCreate()
        (application as TariWalletApplication).appComponent.inject(this)
    }

    /**
     * Called on service start-up.
     */
    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // start wallet manager & listen to events
        EventBus.subscribeToWalletState(this, this::onWalletStateChanged)
        walletManager.start()
        // start service & post foreground service notification
        val notification = notificationHelper.buildForegroundServiceNotification()
        startForeground(NOTIFICATION_ID, notification)
        Logger.d("Tari wallet service started.")
        return START_NOT_STICKY
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.RUNNING) {
            wallet = FFIWallet.instance!!
            wallet.listenerAdapter = this
            EventBus.unsubscribeFromWalletState(this)
            ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        }
    }

    /**
     * Bound service.
     */
    override fun onBind(intent: Intent?): IBinder? {
        Logger.d("Service bound.")
        return serviceImpl
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Logger.d("Service unbound.")
        return super.onUnbind(intent)
    }

    /**
     * A broadcast is made on destroy to get the service running again.
     */
    override fun onDestroy() {
        Logger.d("Service destroyed.")
        txExpirationCheckSubscription.dispose()
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

    override fun onTxBroadcast(completedTx: CompletedTx) {
        Logger.d("Tx ${completedTx.id} broadcast.")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxBroadcast(completedTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxBroadcast(completedTx)
        }
    }

    override fun onTxMined(completedTx: CompletedTx) {
        Logger.d("Tx ${completedTx.id} mined.")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxMined(completedTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxMined(completedTx)
        }
    }

    override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
        Logger.d("Tx received: $pendingInboundTx")
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        Logger.d("Received TX after contact update: $pendingInboundTx")
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxReceived(pendingInboundTx))
        // manage notifications
        postTxNotification(pendingInboundTx)
        listeners.forEach { it.onTxReceived(pendingInboundTx) }
    }

    override fun onTxReplyReceived(completedTx: CompletedTx) {
        Logger.d("Tx ${completedTx.id} reply received.")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxReplyReceived(completedTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxReplyReceived(completedTx)
        }
    }

    override fun onTxFinalized(completedTx: CompletedTx) {
        Logger.d("Tx ${completedTx.id} finalized.")
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the internal listeners
        EventBus.post(Event.Wallet.TxFinalized(completedTx))
        // notify external listeners
        listeners.iterator().forEach {
            it.onTxFinalized(completedTx)
        }
    }

    override fun onDirectSendResult(txId: BigInteger, success: Boolean) {
        Logger.d("Tx $txId direct send completed. Success: $success")
        // post event to bus
        EventBus.post(Event.Wallet.DirectSendResult(TxId(txId), success))
        if (success) {
            val txIdPublicKeyPair = outboundTxIdsToBePushNotified.firstOrNull { it.first == txId }
            if (txIdPublicKeyPair != null) {
                outboundTxIdsToBePushNotified.removeIf {
                    it.first == txId
                }
                sendPushNotificationToTxRecipient(txIdPublicKeyPair.second)
            }
        }
        // notify external listeners
        listeners.iterator().forEach {
            it.onDirectSendResult(TxId(txId), success)
        }
    }

    override fun onStoreAndForwardSendResult(txId: BigInteger, success: Boolean) {
        Logger.d("Tx $txId store and forward send completed. Success: $success")
        // post event to bus
        EventBus.post(Event.Wallet.StoreAndForwardSendResult(TxId(txId), success))
        if (success) {
            val txIdPublicKeyPair = outboundTxIdsToBePushNotified.firstOrNull { it.first == txId }
            if (txIdPublicKeyPair != null) {
                outboundTxIdsToBePushNotified.removeIf {
                    it.first == txId
                }
                sendPushNotificationToTxRecipient(txIdPublicKeyPair.second)
            }
        }
        // notify external listeners
        listeners.iterator().forEach {
            it.onStoreAndForwardSendResult(TxId(txId), success)
        }
    }

    override fun onTxCancelled(cancelledTx: CancelledTx) {
        Logger.d("Tx cancelled: $cancelledTx")
        val error = WalletError()
        // TODO re-fetch the tx because of an FFI bug that causes the recipient public key to
        //  be all 0s, will revert once the FFI bug is fixed
        val refetchedCancelledTx = serviceImpl.getCancelledTxById(TxId(cancelledTx.id), error)
        Logger.d("Cancelled tx refetched: $refetchedCancelledTx")
        refetchedCancelledTx?.let {
            // post event to bus
            EventBus.post(Event.Wallet.TxCancelled(it))
            // notify external listeners
            if (it.direction == INBOUND &&
                !(app.isInForeground && app.currentActivity is HomeActivity)
            ) {
                Logger.i("Posting cancellation notification")
                notificationHelper.postTxCanceledNotification(it)
            }
            listeners.iterator().forEach { listener -> listener.onTxCancellation(it) }
        }
    }

    override fun onBaseNodeSyncComplete(requestId: BigInteger, success: Boolean) {
        Logger.d("Request $requestId base node sync complete. Success: $success")
        // post event to bus
        EventBus.post(Event.Wallet.BaseNodeSyncComplete(RequestId(requestId), success))
        // notify external listeners
        listeners.iterator().forEach {
            it.onBaseNodeSyncComplete(RequestId(requestId), success)
        }
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
        // if app is backgrounded, display heads-up notification
        if (!app.isInForeground || app.currentActivity !is HomeActivity) {
            notificationHelper.postCustomLayoutTxNotification(tx)
        }
    }

    private fun sendPushNotificationToTxRecipient(recipientPublicKeyHex: String) {
        // the push notification server accepts lower-case hex strings as of now
        val fromPublicKeyHex = wallet.getPublicKey().toString().toLowerCase(Locale.ENGLISH)
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

        /**
         * Maps the throwable into the error out parameter.
         */
        private fun mapThrowableIntoError(throwable: Throwable, error: WalletError) {
            error.code = WalletErrorCode.UNKNOWN_ERROR
            error.message = throwable.message
            if (throwable is FFIException) {
                if (throwable.error != null) {
                    error.code = WalletErrorCode.fromCode(throwable.error.code)
                }
            }
        }

        private fun getContactByPublicKeyHexString(
            allContacts: List<Contact>,
            hexString: String
        ): Contact? = allContacts.firstOrNull { it.publicKey.hexString == hexString }

        override fun registerListener(listener: TariWalletServiceListener): Boolean {
            listeners.add(listener)
            listener.asBinder().linkToDeath({
                val removeSuccessful = listeners.remove(listener)
                Logger.i("Listener died. Remove successful: $removeSuccessful.")
            }, 0)
            return true
        }

        override fun unregisterListener(listener: TariWalletServiceListener): Boolean {
            return listeners.remove(listener)
        }

        override fun getPublicKeyHexString(error: WalletError): String? {
            return try {
                wallet.getPublicKey().toString()
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Wallet balance info.
         */
        override fun getBalanceInfo(error: WalletError): BalanceInfo? {
            return try {
                BalanceInfo(
                    MicroTari(wallet.getAvailableBalance()),
                    MicroTari(wallet.getPendingIncomingBalance()),
                    MicroTari(wallet.getPendingOutgoingBalance())
                )
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Get all contacts.
         */
        override fun getContacts(error: WalletError): List<Contact>? {
            try {
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
                return contacts.sortedWith(compareBy { it.alias })
            } catch (throwable: Throwable) {
                error.code = WalletErrorCode.UNKNOWN_ERROR
                error.message = throwable.message
                if (throwable is FFIException) {
                    if (throwable.error != null) {
                        error.code = WalletErrorCode.fromCode(throwable.error.code)
                    }
                }
                return null
            }
        }

        /**
         * Gets all users that this wallet had a transaction with, and returns a list
         * of most recent ones, limited by the limit parameter.
         */
        override fun getRecentTxUsers(maxCount: Int, error: WalletError): MutableList<User>? {
            // pre-fetch contacs
            val allContacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || allContacts == null) {
                return null
            }
            val txs = ArrayList<Tx>()
            // collect all transactions
            txs.addAll(getPendingInboundTxs(allContacts))
            txs.addAll(getPendingOutboundTxs(allContacts))
            txs.addAll(getCompletedTxs(allContacts))
            txs.addAll(getCancelledTxs(allContacts))
            // sort them by descending timestamp
            val sortedTxs = txs.sortedWith(compareByDescending { it.timestamp })
            val recentTxUsers = mutableListOf<User>()

            for (tx in sortedTxs) {
                if (recentTxUsers.size >= maxCount) { // comes first for the case of (maxCount <= 0)
                    break
                }
                if (!recentTxUsers.contains(tx.user)) {
                    val txUser = getContactByPublicKeyHexString(
                        allContacts,
                        tx.user.publicKey.hexString
                    ) ?: tx.user
                    recentTxUsers.add(txUser)
                }
            }
            return recentTxUsers
        }

        /**
         * Internal function to get all completed transactions.
         *
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getCompletedTxs(allContacts: List<Contact>): List<CompletedTx> {
            val completedTxsFFI = wallet.getCompletedTxs()
            val completedTxs = mutableListOf<CompletedTx>()
            for (i in 0 until completedTxsFFI.getLength()) {
                val completedTxFFI = completedTxsFFI.getAt(i)
                val completedTx = completedTxFromFFI(completedTxFFI, allContacts)
                completedTxFFI.destroy()
                completedTxs.add(completedTx)
            }
            // destroy native collection
            completedTxsFFI.destroy()
            return completedTxs
        }


        /**
         * Get all completed transactions.
         * Client-facing function.
         */
        override fun getCompletedTxs(error: WalletError): List<CompletedTx>? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getCompletedTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        private fun getCancelledTxs(contacts: List<Contact>): List<CancelledTx> {
            val canceledTxsFFI = wallet.getCancelledTxs()
            return (0 until canceledTxsFFI.getLength())
                .map {
                    val txFFI = canceledTxsFFI.getAt(it)
                    cancelledTxFromFFI(txFFI, contacts).also { txFFI.destroy() }
                }.also { canceledTxsFFI.destroy() }
        }

        override fun getCancelledTxs(error: WalletError): List<CancelledTx>? {
            val contacts = getContacts(error)
            return if (error.code != WalletErrorCode.NO_ERROR || contacts == null) null
            else try {
                getCancelledTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a completed transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getCancelledTxById(id: TxId, allContacts: List<Contact>): CancelledTx {
            val completedTxFFI = wallet.getCancelledTxById(id.value)
            val cancelledTx = cancelledTxFromFFI(completedTxFFI, allContacts)
            completedTxFFI.destroy()
            return cancelledTx
        }

        /**
         * Get completed transaction by id.
         * Client-facing function.
         */
        override fun getCancelledTxById(id: TxId, error: WalletError): CancelledTx? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getCancelledTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a completed transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getCompletedTxById(id: TxId, allContacts: List<Contact>): CompletedTx {
            val completedTxFFI = wallet.getCompletedTxById(id.value)
            val completedTx = completedTxFromFFI(completedTxFFI, allContacts)
            completedTxFFI.destroy()
            return completedTx
        }

        /**
         * Get completed transaction by id.
         * Client-facing function.
         */
        override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getCompletedTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get all pending inbound transactions.
         *
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingInboundTxs(allContacts: List<Contact>): List<PendingInboundTx> {
            val pendingInboundTxsFFI = wallet.getPendingInboundTxs()
            val pendingInboundTxs = mutableListOf<PendingInboundTx>()
            for (i in 0 until pendingInboundTxsFFI.getLength()) {
                val pendingInboundTxFFI = pendingInboundTxsFFI.getAt(i)
                val pendingInboundTx = pendingInboundTxFromFFI(pendingInboundTxFFI, allContacts)
                pendingInboundTxFFI.destroy()
                pendingInboundTxs.add(pendingInboundTx)
            }
            // destroy native collection
            pendingInboundTxsFFI.destroy()
            return pendingInboundTxs
        }

        /**
         * Get all pending inbound transactions.
         * Client-facing function.
         */
        override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? {
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingInboundTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a pending inbound transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingInboundTxById(
            id: TxId,
            allContacts: List<Contact>
        ): PendingInboundTx {
            val pendingInboundTxFFI = wallet.getPendingInboundTxById(id.value)
            val pendingInboundTx = pendingInboundTxFromFFI(pendingInboundTxFFI, allContacts)
            pendingInboundTxFFI.destroy()
            return pendingInboundTx
        }

        /**
         * Get pending inbound transaction by id.
         * Client-facing function.
         */
        override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? {
            // call the corresponding function with fresh contacts list
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingInboundTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get all pending outbound transactions.
         *
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingOutboundTxs(allContacts: List<Contact>): List<PendingOutboundTx> {
            val pendingOutboundTxsFFI = wallet.getPendingOutboundTxs()
            val pendingOutboundTxs = mutableListOf<PendingOutboundTx>()
            for (i in 0 until pendingOutboundTxsFFI.getLength()) {
                val pendingOutboundTxFFI = pendingOutboundTxsFFI.getAt(i)
                val pendingOutboundTx = pendingOutboundTxFromFFI(pendingOutboundTxFFI, allContacts)
                pendingOutboundTxFFI.destroy()
                pendingOutboundTxs.add(pendingOutboundTx)
            }
            // destroy native collection
            pendingOutboundTxsFFI.destroy()
            return pendingOutboundTxs
        }

        /**
         * Get all pending outbound transactions.
         * Client-facing function.
         */
        override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? {
            // call the corresponding function with fresh contacts list
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingOutboundTxs(contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        /**
         * Internal function to get a pending outbound transaction by id.
         *
         * @param id tx id
         * @param allContacts pre-fetched list of all contacts
         */
        private fun getPendingOutboundTxById(
            id: TxId,
            allContacts: List<Contact>
        ): PendingOutboundTx {
            val pendingOutboundTxFFI = wallet.getPendingOutboundTxById(id.value)
            val pendingOutboundTx = pendingOutboundTxFromFFI(pendingOutboundTxFFI, allContacts)
            pendingOutboundTxFFI.destroy()
            return pendingOutboundTx
        }

        /**
         * Get pending outbound transaction by id.
         * Client-facing function.
         */
        override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? {
            // call the corresponding function with fresh contacts list
            val contacts = getContacts(error)
            if (error.code != WalletErrorCode.NO_ERROR || contacts == null) {
                return null
            }
            return try {
                getPendingOutboundTxById(id, contacts)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        override fun cancelPendingTx(id: TxId, error: WalletError): Boolean {
            return try {
                wallet.cancelPendingTx(id.value)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                false
            }
        }

        override fun syncWithBaseNode(error: WalletError): RequestId? {
            return try {
                RequestId(wallet.syncWithBaseNode())
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        override fun sendTari(
            user: User,
            amount: MicroTari,
            fee: MicroTari,
            message: String,
            error: WalletError
        ): TxId? {
            return try {
                val recipientPublicKeyHex = user.publicKey.hexString
                val publicKeyFFI = FFIPublicKey(HexString(recipientPublicKeyHex))
                val txId = wallet.sendTx(
                    publicKeyFFI,
                    amount.value,
                    fee.value,
                    message
                )
                publicKeyFFI.destroy()
                outboundTxIdsToBePushNotified.add(
                    Pair(txId, recipientPublicKeyHex.toLowerCase(Locale.ENGLISH))
                )
                TxId(txId)
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
                null
            }
        }

        // region FFI to model extraction functions
        private fun publicKeyFromFFI(
            publicKeyFFI: FFIPublicKey
        ): PublicKey {
            return PublicKey(
                publicKeyFFI.toString(),
                publicKeyFFI.getEmojiNodeId()
            )
        }

        private fun completedTxFromFFI(
            completedTxFFI: FFICompletedTx,
            allContacts: List<Contact>
        ): CompletedTx {
            val sourcePublicKeyFFI = completedTxFFI.getSourcePublicKey()
            val destinationPublicKeyFFI = completedTxFFI.getDestinationPublicKey()
            val status = when (completedTxFFI.getStatus()) {
                FFITxStatus.TX_NULL_ERROR -> TxStatus.TX_NULL_ERROR
                FFITxStatus.BROADCAST -> TxStatus.BROADCAST
                FFITxStatus.COMPLETED -> TxStatus.COMPLETED
                FFITxStatus.MINED -> TxStatus.MINED
                FFITxStatus.IMPORTED -> TxStatus.IMPORTED
                FFITxStatus.PENDING -> TxStatus.PENDING
                FFITxStatus.UNKNOWN -> TxStatus.UNKNOWN
            }
            val user: User
            val direction: Tx.Direction

            // get public key
            val error = WalletError()
            if (error.code != WalletErrorCode.NO_ERROR) {
                throw FFIException(message = error.message)
            }

            if (wallet.isCompletedTxOutbound(completedTxFFI)) {
                direction = OUTBOUND
                val userPublicKey = PublicKey(
                    destinationPublicKeyFFI.toString(),
                    destinationPublicKeyFFI.getEmojiNodeId()
                )
                user = getContactByPublicKeyHexString(
                    allContacts,
                    destinationPublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            } else {
                direction = INBOUND
                val userPublicKey = PublicKey(
                    sourcePublicKeyFFI.toString(),
                    sourcePublicKeyFFI.getEmojiNodeId()
                )
                user = getContactByPublicKeyHexString(
                    allContacts,
                    sourcePublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            }

            val completedTx = CompletedTx(
                completedTxFFI.getId(),
                direction,
                user,
                MicroTari(completedTxFFI.getAmount()),
                MicroTari(completedTxFFI.getFee()),
                completedTxFFI.getTimestamp(),
                completedTxFFI.getMessage(),
                status
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            destinationPublicKeyFFI.destroy()
            return completedTx
        }

        private fun cancelledTxFromFFI(
            completedTxFFI: FFICompletedTx,
            allContacts: List<Contact>
        ): CancelledTx {
            val sourcePublicKeyFFI = completedTxFFI.getSourcePublicKey()
            val destinationPublicKeyFFI = completedTxFFI.getDestinationPublicKey()
            val status = when (completedTxFFI.getStatus()) {
                FFITxStatus.TX_NULL_ERROR -> TxStatus.TX_NULL_ERROR
                FFITxStatus.BROADCAST -> TxStatus.BROADCAST
                FFITxStatus.COMPLETED -> TxStatus.COMPLETED
                FFITxStatus.MINED -> TxStatus.MINED
                FFITxStatus.IMPORTED -> TxStatus.IMPORTED
                FFITxStatus.PENDING -> TxStatus.PENDING
                FFITxStatus.UNKNOWN -> TxStatus.UNKNOWN
            }
            val user: User
            val direction: Tx.Direction

            // get public key
            val error = WalletError()
            if (error.code != WalletErrorCode.NO_ERROR) {
                throw FFIException(message = error.message)
            }

            if (wallet.isCompletedTxOutbound(completedTxFFI)) {
                direction = OUTBOUND
                val userPublicKey = PublicKey(
                    destinationPublicKeyFFI.toString(),
                    destinationPublicKeyFFI.getEmojiNodeId()
                )
                user = getContactByPublicKeyHexString(
                    allContacts,
                    destinationPublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            } else {
                direction = INBOUND
                val userPublicKey = PublicKey(
                    sourcePublicKeyFFI.toString(),
                    sourcePublicKeyFFI.getEmojiNodeId()
                )
                user = getContactByPublicKeyHexString(
                    allContacts,
                    sourcePublicKeyFFI.toString()
                ) ?: User(userPublicKey)
            }
            val tx = CancelledTx(
                completedTxFFI.getId(),
                direction,
                user,
                MicroTari(completedTxFFI.getAmount()),
                MicroTari(completedTxFFI.getFee()),
                completedTxFFI.getTimestamp(),
                completedTxFFI.getMessage(),
                status
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            destinationPublicKeyFFI.destroy()
            if (status != TxStatus.UNKNOWN) {
                Logger.d("Canceled TX's status is not UNKNOWN but rather $status.\n$tx")
            }
            return tx
        }

        private fun pendingInboundTxFromFFI(
            pendingInboundTxFFI: FFIPendingInboundTx,
            allContacts: List<Contact>
        ): PendingInboundTx {
            val status = when (pendingInboundTxFFI.getStatus()) {
                FFITxStatus.TX_NULL_ERROR -> TxStatus.TX_NULL_ERROR
                FFITxStatus.BROADCAST -> TxStatus.BROADCAST
                FFITxStatus.COMPLETED -> TxStatus.COMPLETED
                FFITxStatus.MINED -> TxStatus.MINED
                FFITxStatus.IMPORTED -> TxStatus.IMPORTED
                FFITxStatus.PENDING -> TxStatus.PENDING
                FFITxStatus.UNKNOWN -> TxStatus.UNKNOWN
            }
            val sourcePublicKeyFFI = pendingInboundTxFFI.getSourcePublicKey()
            val userPublicKey = PublicKey(
                sourcePublicKeyFFI.toString(),
                sourcePublicKeyFFI.getEmojiNodeId()
            )
            val user = getContactByPublicKeyHexString(
                allContacts,
                sourcePublicKeyFFI.toString()
            ) ?: User(userPublicKey)
            val pendingInboundTx = PendingInboundTx(
                pendingInboundTxFFI.getId(),
                user,
                MicroTari(pendingInboundTxFFI.getAmount()),
                pendingInboundTxFFI.getTimestamp(),
                pendingInboundTxFFI.getMessage(),
                status
            )
            // destroy native objects
            sourcePublicKeyFFI.destroy()
            return pendingInboundTx
        }

        private fun pendingOutboundTxFromFFI(
            pendingOutboundTxFFI: FFIPendingOutboundTx,
            allContacts: List<Contact>
        ): PendingOutboundTx {
            val status = when (pendingOutboundTxFFI.getStatus()) {
                FFITxStatus.TX_NULL_ERROR -> TxStatus.TX_NULL_ERROR
                FFITxStatus.BROADCAST -> TxStatus.BROADCAST
                FFITxStatus.COMPLETED -> TxStatus.COMPLETED
                FFITxStatus.MINED -> TxStatus.MINED
                FFITxStatus.IMPORTED -> TxStatus.IMPORTED
                FFITxStatus.PENDING -> TxStatus.PENDING
                FFITxStatus.UNKNOWN -> TxStatus.UNKNOWN
            }
            val destinationPublicKeyFFI = pendingOutboundTxFFI.getDestinationPublicKey()
            val userPublicKey = PublicKey(
                destinationPublicKeyFFI.toString(),
                destinationPublicKeyFFI.getEmojiNodeId()
            )
            val user = getContactByPublicKeyHexString(
                allContacts,
                destinationPublicKeyFFI.toString()
            ) ?: User(userPublicKey)
            val pendingOutboundTx = PendingOutboundTx(
                pendingOutboundTxFFI.getId(),
                user,
                MicroTari(pendingOutboundTxFFI.getAmount()),
                MicroTari(pendingOutboundTxFFI.getFee()),
                pendingOutboundTxFFI.getTimestamp(),
                pendingOutboundTxFFI.getMessage(),
                status
            )
            // destroy native objects
            destinationPublicKeyFFI.destroy()
            return pendingOutboundTx
        }

        @SuppressLint("CheckResult")
        override fun requestTestnetTari(error: WalletError) {
            if (
                serviceImpl.getCompletedTxs(emptyList()).isNotEmpty() ||
                serviceImpl.getPendingInboundTxs(emptyList()).isNotEmpty() ||
                serviceImpl.getPendingOutboundTxs(emptyList()).isNotEmpty()
            ) return
            // get public key
            val publicKeyHexString = getPublicKeyHexString(error)
            if (error.code != WalletErrorCode.NO_ERROR || publicKeyHexString == null) {
                notifyTestnetTariRequestFailed("Service error.")
                return
            }

            val message = "$MESSAGE_PREFIX $publicKeyHexString"
            val signing = wallet.signMessage(message)
            val signature = signing.split("|")[0]
            val nonce = signing.split("|")[1]

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
                    sharedPrefsWrapper.testnetTariUTXOKeyList = result.keys

                    // post event to bus for the internal listeners
                    EventBus.post(Event.Testnet.TestnetTariRequestSuccessful())
                    // notify external listeners
                    listeners.iterator().forEach { it.onTestnetTariRequestSuccess() }
                },
                {
                    Logger.i("requestMaxTestnetTari error $it")
                    error.code = WalletErrorCode.UNKNOWN_ERROR
                    val errorMessage =
                        string(R.string.wallet_service_error_testnet_tari_request) +
                                " " +
                                it.message
                    error.message = errorMessage
                    if (it is TestnetTariRequestException) {
                        notifyTestnetTariRequestFailed(errorMessage)
                    } else {
                        notifyTestnetTariRequestFailed(string(R.string.wallet_service_error_no_internet_connection))
                    }
                }
            )
        }

        override fun importTestnetUTXO(txMessage: String, error: WalletError): CompletedTx? {
            val keys = sharedPrefsWrapper.testnetTariUTXOKeyList.toMutableList()
            if (keys.isEmpty()) {
                return null
            }
            val firstUTXOKey = keys.first()
            val senderPublicKeyFFI = FFIPublicKey(HexString(firstUTXOKey.senderPublicKeyHex!!))
            val txId: BigInteger
            FFIPrivateKey(HexString(firstUTXOKey.key)).also { spendingPrivateKeyFFI ->
                val amount = BigInteger(firstUTXOKey.value)
                txId = wallet.importUTXO(
                    amount,
                    txMessage,
                    spendingPrivateKeyFFI,
                    senderPublicKeyFFI
                )
                spendingPrivateKeyFFI.destroy()
            }
            senderPublicKeyFFI.destroy()
            // remove the used key
            keys.remove(firstUTXOKey)
            sharedPrefsWrapper.testnetTariUTXOKeyList = keys
            // get transaction and post notification
            val tx = getCompletedTxById(TxId(txId), error)
            if (error.code != WalletErrorCode.NO_ERROR || tx == null) {
                return null
            }
            postTxNotification(tx)
            return tx
        }

        override fun removeContact(contact: Contact, error: WalletError): Boolean {
            try {
                val contactsFFI = wallet.getContacts()
                for (i in 0 until contactsFFI.getLength()) {
                    val contactFFI = contactsFFI.getAt(i)
                    val publicKeyFFI = contactFFI.getPublicKey()
                    if (publicKeyFFI.toString() == contact.publicKey.hexString) {
                        val result = wallet.removeContact(contactFFI)
                        publicKeyFFI.destroy()
                        contactFFI.destroy()
                        contactsFFI.destroy()
                        return result
                    }
                    publicKeyFFI.destroy()
                    contactFFI.destroy()
                }
                contactsFFI.destroy()
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
            }
            return false
        }

        private fun notifyTestnetTariRequestFailed(error: String) {
            // post event to bus for the internal listeners
            EventBus.post(Event.Testnet.TestnetTariRequestError(error))
            // notify external listeners
            listeners.iterator().forEach { listener ->
                listener.onTestnetTariRequestError(error)
            }
        }

        override fun updateContactAlias(
            publicKey: PublicKey,
            alias: String,
            error: WalletError
        ) {
            try {
                val publicKeyFFI = FFIPublicKey(HexString(publicKey.hexString))
                val contact = FFIContact(alias, publicKeyFFI)
                wallet.addUpdateContact(contact)
                publicKeyFFI.destroy()
                contact.destroy()
            } catch (throwable: Throwable) {
                mapThrowableIntoError(throwable, error)
            }
        }

        /**
         * @return public key constructed from input emoji id. Null if the emoji id is invalid
         * or it does not correspond to a public key.
         */
        override fun getPublicKeyFromEmojiId(emojiId: String?): PublicKey? {
            try {
                FFIPublicKey(emojiId ?: "").run {
                    val publicKey = publicKeyFromFFI(this)
                    destroy()
                    return publicKey
                }
            } catch (ignored: Throwable) {
                return null
            }
        }

        /**
         * @return public key constructed from input public key hex string id. Null if the emoji id
         * is invalid or it does not correspond to a public key.
         */
        override fun getPublicKeyFromHexString(publicKeyHex: String?): PublicKey? {
            try {
                FFIPublicKey(HexString(publicKeyHex ?: "")).run {
                    val publicKey = publicKeyFromFFI(this)
                    destroy()
                    return publicKey
                }
            } catch (ignored: Throwable) {
                return null
            }
        }

        // endregion
    }
}
