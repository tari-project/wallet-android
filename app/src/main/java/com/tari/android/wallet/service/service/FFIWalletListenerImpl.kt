package com.tari.android.wallet.service.service

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodePrefRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.FFIWalletListener
import com.tari.android.wallet.ffi.TransactionValidationStatus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TransactionSendStatus
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

class FFIWalletListenerImpl(
    private val wallet: FFIWallet,
    private val backupManager: BackupManager,
    private val notificationHelper: NotificationHelper,
    private val notificationService: NotificationService,
    private val app: TariWalletApplication,
    private val baseNodeSharedPrefsRepository: BaseNodePrefRepository,
    private val baseNodesManager: BaseNodesManager
) : FFIWalletListener {

    private val logger
        get() = Logger.t("FFIWalletListenerImpl")
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

    val outboundTxIdsToBePushNotified = CopyOnWriteArraySet<OutboundTxNotification>()

    override fun onTxReceived(pendingInboundTx: PendingInboundTx) {
        val newTx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxReceived(newTx))
        // manage notifications
        postTxNotification(newTx)
        listeners.forEach { it.onTxReceived(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) {
        val newTx = pendingOutboundTx.copy(tariContact = getUserByWalletAddress(pendingOutboundTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxReplyReceived(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxReplyReceived(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxFinalized(pendingInboundTx: PendingInboundTx) {
        val newTx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxFinalized(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFinalized(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) {
        val newTx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.InboundTxBroadcast(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onInboundTxBroadcast(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) {
        val newTx = pendingOutboundTx.copy(tariContact = getUserByWalletAddress(pendingOutboundTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.OutboundTxBroadcast(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onOutboundTxBroadcast(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxMined(completedTx: CompletedTx) {
        val newTx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxMined(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxMined(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
        val newTx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxMinedUnconfirmed(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxMinedUnconfirmed(newTx, confirmationCount) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxFauxConfirmed(completedTx: CompletedTx) {
        val newTx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxFauxConfirmed(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFauxConfirmed(newTx) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) {
        val newTx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress))
        // post event to bus for the listeners
        EventBus.post(Event.Transaction.TxFauxMinedUnconfirmed(newTx))
        // notify external listeners
        listeners.iterator().forEach { it.onTxFauxUnconfirmed(newTx, confirmationCount) }
        // schedule a backup
        backupManager.backupNow()
    }

    override fun onDirectSendResult(txId: BigInteger, status: TransactionSendStatus) {
        // post event to bus
        EventBus.post(Event.Transaction.DirectSendResult(TxId(txId), status))
        outboundTxIdsToBePushNotified.firstOrNull { it.txId == txId }?.let {
            outboundTxIdsToBePushNotified.remove(it)
            sendPushNotificationToTxRecipient(it.recipientPublicKeyHex)
        }
        // schedule a backup
        backupManager.backupNow()
        // notify external listeners
        listeners.iterator().forEach { it.onDirectSendResult(TxId(txId), status) }
    }

    override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) {
        val newTx = cancelledTx.copy(tariContact = getUserByWalletAddress(cancelledTx.tariContact.walletAddress))
        // post event to bus
        EventBus.post(Event.Transaction.TxCancelled(newTx))
        val currentActivity = app.currentActivity
        if (cancelledTx.direction == Tx.Direction.INBOUND && !(app.isInForeground && currentActivity is HomeActivity && currentActivity.willNotifyAboutNewTx())
        ) {
            notificationHelper.postTxCanceledNotification(newTx)
        }
        // notify external listeners
        listeners.iterator().forEach { listener -> listener.onTxCancelled(newTx) }
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
        when (ConnectivityStatus.entries[status]) {
            ConnectivityStatus.CONNECTING -> {
                /* do nothing */
            }

            ConnectivityStatus.ONLINE -> {
                baseNodesManager.refreshBaseNodeList()
                baseNodeSharedPrefsRepository.baseNodeState = BaseNodeState.Online
                EventBus.baseNodeState.post(BaseNodeState.Online)
                listeners.iterator().forEach { it.onBaseNodeSyncComplete(true) }
            }

            ConnectivityStatus.OFFLINE -> {
                val currentBaseNode = baseNodeSharedPrefsRepository.currentBaseNode
                if (currentBaseNode == null || !currentBaseNode.isCustom) {
                    baseNodesManager.setNextBaseNode()
                    baseNodesManager.startSync()
                }
                baseNodeSharedPrefsRepository.baseNodeState = BaseNodeState.Offline
                EventBus.baseNodeState.post(BaseNodeState.Offline)
                listeners.iterator().forEach { it.onBaseNodeSyncComplete(false) }
            }

        }
    }

    private fun getUserByWalletAddress(address: TariWalletAddress): TariContact {
        val contactsFFI = wallet.getContacts()
        for (i in 0 until contactsFFI.getLength()) {
            val contactFFI = contactsFFI.getAt(i)
            val walletAddressFFI = contactFFI.getWalletAddress()
            val tariContact = if (TariWalletAddress(walletAddressFFI) == address) TariContact(contactFFI) else null
            walletAddressFFI.destroy()
            contactFFI.destroy()
            if (tariContact != null) {
                contactsFFI.destroy()
                return tariContact
            }
        }
        // destroy native collection
        contactsFFI.destroy()
        return TariContact(address)
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

    private fun sendPushNotificationToTxRecipient(recipientHex: String) {
        val senderHex = wallet.getWalletAddress().notificationHex()
        notificationService.notifyRecipient(recipientHex, senderHex, wallet::signMessage)
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
                baseNodesManager.setNextBaseNode()
                baseNodesManager.startSync()
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
        try {
            val currentStatus = baseNodeValidationStatusMap[type] ?: return
            if (currentStatus.first != responseId) return
            baseNodeValidationStatusMap[type] = Pair(currentStatus.first, isSuccess)
            checkBaseNodeSyncCompletion()
        } catch (e: Throwable) {
            logger.i(e.toString())
        }
    }

    override fun onWalletRestoration(result: WalletRestorationResult) {
        EventBus.walletRestorationState.post(result)
    }

    override fun onBaseNodeStateChanged(baseNodeState: FFITariBaseNodeState) {
        baseNodesManager.saveBaseNodeState(baseNodeState)
    }

    enum class ConnectivityStatus(val value: Int) {
        CONNECTING(0),
        ONLINE(1),
        OFFLINE(2),
    }

    data class OutboundTxNotification(val txId: BigInteger, val recipientPublicKeyHex: String)
}