package com.tari.android.wallet.service.service

import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.baseNodes.BaseNodes
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.FFIWalletListener
import com.tari.android.wallet.ffi.TransactionValidationStatus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.TariWalletServiceListener
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.math.BigInteger
import java.util.concurrent.*

class FFIWalletListenerImpl(
    private val wallet: FFIWallet,
    private val backupManager: BackupManager,
    private val notificationHelper: NotificationHelper,
    private val notificationService: NotificationService,
    private val app: TariWalletApplication,
    private val baseNodeSharedPrefsRepository: BaseNodeSharedRepository,
    private val baseNodes: BaseNodes
) : FFIWalletListener {

    var listeners = CopyOnWriteArrayList<TariWalletServiceListener>()

    /**
     * Maps the validation type to the request id and validation result. This map will be
     * initialized at the beginning of each base node validation sequence.
     * Validation results will all be null, and will be set as the result callbacks get called.
     */
    var baseNodeValidationStatusMap: ConcurrentMap<BaseNodeValidationType, Pair<BigInteger, Boolean?>> = ConcurrentHashMap()

    /**
     * Debounce for inbound transaction notification.
     */
    private var txReceivedNotificationDelayedAction: Disposable? = null
    private var inboundTxEventNotificationTxs = mutableListOf<Tx>()

    private var txBroadcastRestarted = false

    /**
     * Pairs of <tx id, recipient public key hex>.
     */
    val outboundTxIdsToBePushNotified = CopyOnWriteArraySet<Pair<BigInteger, String>>()

    override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxReceived(pendingInboundTx))
        // manage notifications
        postTxNotification(pendingInboundTx)
        listeners.forEach { it.onTxReceived(pendingInboundTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) {
        pendingOutboundTx.user = getUserByPublicKey(pendingOutboundTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxReplyReceived(pendingOutboundTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxReplyReceived(pendingOutboundTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxFinalized(pendingInboundTx: PendingInboundTx) {
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxFinalized(pendingInboundTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFinalized(pendingInboundTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) {
        pendingInboundTx.user = getUserByPublicKey(pendingInboundTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.InboundTxBroadcast(pendingInboundTx))
        // notify external listeners
        listeners.iterator().forEach { it.onInboundTxBroadcast(pendingInboundTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) {
        pendingOutboundTx.user = getUserByPublicKey(pendingOutboundTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.OutboundTxBroadcast(pendingOutboundTx))
        // notify external listeners
        listeners.iterator().forEach { it.onOutboundTxBroadcast(pendingOutboundTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxMined(completedTx: CompletedTx) {
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxMined(completedTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxMined(completedTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxMinedUnconfirmed(completedTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxMinedUnconfirmed(completedTx, confirmationCount) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxFauxConfirmed(completedTx: CompletedTx) {
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxFauxConfirmed(completedTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFauxConfirmed(completedTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
        completedTx.user = getUserByPublicKey(completedTx.user.publicKey)
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxFauxMinedUnconfirmed(completedTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFauxUnconfirmed(completedTx, confirmationCount) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onDirectSendResult(txId: BigInteger, status: TransactionSendStatus) {
        // post event to bus
        EventBus.post(Event.Transaction.DirectSendResult(TxId(txId), status))
        outboundTxIdsToBePushNotified.firstOrNull { it.first == txId }?.let {
            outboundTxIdsToBePushNotified.remove(it)
            sendPushNotificationToTxRecipient(it.second)
        }
        // schedule a backup
        backupManager.backupNow()
        // notify external listeners
        listeners.iterator().forEach { it.onDirectSendResult(TxId(txId), status) }
    }

    override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) {
        cancelledTx.user = getUserByPublicKey(cancelledTx.user.publicKey)
        // post event to bus
        EventBus.post(Event.Transaction.TxCancelled(cancelledTx))
        val currentActivity = app.currentActivity
        if (cancelledTx.direction == Tx.Direction.INBOUND && !(app.isInForeground && currentActivity is HomeActivity && currentActivity.willNotifyAboutNewTx())
        ) {
            notificationHelper.postTxCanceledNotification(cancelledTx)
        }
        // notify external listeners
        listeners.iterator().forEach { listener -> listener.onTxCancelled(cancelledTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTXOValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) {
        checkValidationResult(BaseNodeValidationType.TXO, responseId, status == TransactionValidationStatus.Success)
    }

    override fun onTxValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) {
        checkValidationResult(BaseNodeValidationType.TX, responseId, status == TransactionValidationStatus.Success)
        if (!txBroadcastRestarted && status == TransactionValidationStatus.Success) {
            wallet.restartTxBroadcast()
            txBroadcastRestarted = true
        }
    }

    override fun onBalanceUpdated(balanceInfo: BalanceInfo) {
        EventBus.balanceState.post(balanceInfo)
        // notify external listeners
        listeners.iterator().forEach { it.onBalanceUpdated(balanceInfo) }
    }

    override fun onConnectivityStatus(status: Int) {
        when (status) {
            1 -> {
                baseNodeSharedPrefsRepository.baseNodeState = BaseNodeState.Online.toInt()
                EventBus.baseNodeState.post(BaseNodeState.Online)
                listeners.iterator().forEach { it.onBaseNodeSyncComplete(true) }
            }
            2 -> {
                val currentBaseNode = baseNodeSharedPrefsRepository.currentBaseNode
                if (currentBaseNode == null || !currentBaseNode.isCustom) {
                    baseNodes.setNextBaseNode()
                }
                baseNodeSharedPrefsRepository.baseNodeState = BaseNodeState.Offline.toInt()
                EventBus.baseNodeState.post(BaseNodeState.Offline)
                listeners.iterator().forEach { it.onBaseNodeSyncComplete(false) }
            }
        }
    }

    private fun getUserByPublicKey(key: PublicKey): User {
        val contactsFFI = wallet.getContacts()
        for (i in 0 until contactsFFI.getLength()) {
            val contactFFI = contactsFFI.getAt(i)
            val publicKeyFFI = contactFFI.getPublicKey()
            val hex = publicKeyFFI.toString()
            val contact = if (hex == key.hexString) Contact(key, contactFFI.getAlias()) else null
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

    private fun sendPushNotificationToTxRecipient(recipientPublicKeyHex: String) {
        // the push notification server accepts lower-case hex strings as of now
        //todo remove or get back after turning off faucet
//        val fromPublicKeyHex = wallet.getPublicKey().toString().lowercase(Locale.ENGLISH)
//        notificationService.notifyRecipient(recipientPublicKeyHex, fromPublicKeyHex, wallet::signMessage)
    }

    private fun checkBaseNodeSyncCompletion() {
        // make a copy of the status map for concurrency protection
        val statusMapCopy = baseNodeValidationStatusMap.toMap()
        // if base node not in sync, then switch to the next base node
        // check if any has failed
        val failed = statusMapCopy.any { it.value.second == false }
        if (failed) {
            baseNodeValidationStatusMap.clear()
            baseNodeSharedPrefsRepository.baseNodeLastSyncResult = false
            val currentBaseNode = baseNodeSharedPrefsRepository.currentBaseNode
            if (currentBaseNode == null || !currentBaseNode.isCustom) {
                baseNodes.setNextBaseNode()
            }
            EventBus.baseNodeSyncState.post(BaseNodeSyncState.Failed)
            listeners.iterator().forEach { it.onBaseNodeSyncComplete(false) }
            return
        }
        // if any of the results is null, we're still waiting for all callbacks to happen
        val inProgress = statusMapCopy.any { it.value.second == null }
        if (inProgress) {
            return
        }
        // check if it's successful
        val successful = statusMapCopy.all { it.value.second == true }
        if (successful) {
            baseNodeValidationStatusMap.clear()
            baseNodeSharedPrefsRepository.baseNodeLastSyncResult = true
            EventBus.baseNodeSyncState.post(BaseNodeSyncState.Online)
            listeners.iterator().forEach { it.onBaseNodeSyncComplete(true) }
        }
        // shouldn't ever reach here - no-op
    }

    private fun checkValidationResult(type: BaseNodeValidationType, responseId: BigInteger, isSuccess: Boolean) {
        val currentStatus = baseNodeValidationStatusMap[type] ?: return
        if (currentStatus.first != responseId) return
        baseNodeValidationStatusMap[type] = Pair(currentStatus.first, isSuccess)
        checkBaseNodeSyncCompletion()
    }

    override fun onWalletRestoration(result: WalletRestorationResult) {
        EventBus.walletRestorationState.post(result)
    }
}