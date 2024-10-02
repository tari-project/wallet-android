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
package com.tari.android.wallet.application.walletManager

import com.google.gson.Gson
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFICommsConfig
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import com.tari.android.wallet.ffi.FFITariTransportConfig
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.FFIWalletListener
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ffi.LogFileObserver
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.ffi.TransactionValidationStatus
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.TransactionSendStatus
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.fullBase58
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.service.service.WalletService
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigInteger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utilized to asynchronous manage the sometimes-long-running task of instantiation and start-up
 * of the Tor proxy and the FFI wallet.
 *
 * @author The Tari Development Team
 */
@Singleton
class WalletManager @Inject constructor(
    private val walletConfig: WalletConfig,
    private val torManager: TorProxyManager,
    private val corePrefRepository: CorePrefRepository,
    private val seedPhraseRepository: SeedPhraseRepository,
    private val networkPrefRepository: NetworkPrefRepository,
    private val tariSettingsPrefRepository: TariSettingsPrefRepository,
    private val securityPrefRepository: SecurityPrefRepository,
    private val baseNodesManager: BaseNodesManager,
    private val torConfig: TorConfig,
    private val torProxyStateHandler: TorProxyStateHandler,
    private val app: TariWalletApplication,
    private val notificationHelper: NotificationHelper,
    private val notificationService: NotificationService,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : OutboundTxNotifier {

    private var atomicInstance = AtomicReference<FFIWallet>()
    var walletInstance: FFIWallet?
        get() = atomicInstance.get()
        set(value) = atomicInstance.set(value)

    private val _walletState = MutableStateFlow<WalletState>(WalletState.NotReady)
    val walletState = _walletState.asStateFlow()

    private val _walletEvent = EffectChannelFlow<WalletEvent>()
    val walletEvent: Flow<WalletEvent> = _walletEvent.flow

    private var logFileObserver: LogFileObserver? = null
    private val logger
        get() = Logger.t(WalletManager::class.simpleName)

    /**
     * Maps the validation type to the request id and validation result. This map will be
     * initialized at the beginning of each base node validation sequence.
     * Validation results will all be null, and will be set as the result callbacks get called.
     */
    private val walletValidationStatusMap: ConcurrentMap<WalletValidationType, WalletValidationResult> = ConcurrentHashMap()

    override val outboundTxIdsToBePushNotified = CopyOnWriteArraySet<OutboundTxNotification>()

    /**
     * Debounce for inbound transaction notification.
     * TODO don't use rx. Replace with coroutines.
     */
    private var txReceivedNotificationDelayedAction: Disposable? = null
    private var inboundTxEventNotificationTxs = mutableListOf<Tx>()

    private var txBroadcastRestarted = false

    @Synchronized
    fun start() {
        torManager.run()

        applicationScope.launch {
            torProxyStateHandler.doOnTorReadyForWallet {
                startWallet()
            }
        }
    }

    @Synchronized
    fun stop() {
        // destroy FFI wallet object
        walletInstance?.destroy()
        walletInstance = null
        _walletState.update { WalletState.NotReady }
        // stop tor proxy
        torManager.shutdown()
    }

    fun onWalletStarted() {
        _walletState.update { WalletState.Running }
    }

    fun getCommsConfig(): FFICommsConfig = FFICommsConfig(
        publicAddress = NetAddressString(address = "127.0.0.1", port = 39069).toString(),
        transport = getTorTransport(),
        databaseName = walletConfig.walletDBName,
        datastorePath = walletConfig.getWalletFilesDirPath(),
        discoveryTimeoutSec = Constants.Wallet.DISCOVERY_TIMEOUT_SEC,
        safMessageDurationSec = Constants.Wallet.STORE_AND_FORWARD_MESSAGE_DURATION_SEC,
    )

    /**
     * Syncs the wallet with the base node and validates the wallet
     */
    fun syncBaseNode() {
        var currentBaseNode: BaseNodeDto? = baseNodesManager.currentBaseNode ?: return

        applicationScope.launch(Dispatchers.IO) {
            doOnWalletRunning { wallet ->
                while (currentBaseNode != null) {
                    try {
                        currentBaseNode?.let {
                            logger.i(
                                "startSync:publicKeyHex: ${it.publicKeyHex}\n" +
                                        "startSync:address: ${it.address}\n" +
                                        "startSync:userBaseNodes: ${Gson().toJson(baseNodesManager.userBaseNodes)}"
                            )

                            val baseNodeKeyFFI = FFIPublicKey(HexString(it.publicKeyHex))
                            val addBaseNodeResult = wallet.addBaseNodePeer(baseNodeKeyFFI, it.address)
                            baseNodeKeyFFI.destroy()
                            logger.i("startSync:addBaseNodePeer ${if (addBaseNodeResult) "success" else "failed"}")

                            try {
                                logger.i("startSync:wallet validation:start Tx and TXO validation")
                                walletValidationStatusMap.clear()
                                walletValidationStatusMap[WalletValidationType.TXO] = WalletValidationResult(wallet.startTXOValidation(), null)
                                walletValidationStatusMap[WalletValidationType.TX] = WalletValidationResult(wallet.startTxValidation(), null)
                            } catch (e: Throwable) {
                                logger.i("startSync:wallet validation:error: ${e.message}")
                                walletValidationStatusMap.clear()
                                EventBus.baseNodeSyncState.post(BaseNodeSyncState.Failed)
                            }
                        }
                        break
                    } catch (e: Throwable) {
                        logger.i("startSync:error connecting to base node $currentBaseNode with an error: ${e.message}")
                        currentBaseNode = baseNodesManager.setNextBaseNode()
                    }
                }

                if (currentBaseNode == null) {
                    logger.e("startSync: cannot connect to any base node")
                }
            }
        }
    }

    private fun startWallet() {
        if (walletState.value is WalletState.NotReady || walletState.value is WalletState.Failed) {
            logger.i("Initialize wallet started")
            _walletState.update { WalletState.Initializing }
            applicationScope.launch {
                try {
                    initWallet()
                    _walletState.update { WalletState.Started }
                    logger.i("Wallet was started")
                } catch (e: Exception) {
                    val oldCode = walletState.value.errorCode
                    val newCode = e.safeCastTo<FFIException>()?.error?.code

                    if (oldCode == null || oldCode != newCode) {
                        logger.e(e, "Wallet was failed")
                    }
                    _walletState.update { WalletState.Failed(e) }
                }
            }.start()
        }
    }

    private fun getTorTransport(): FFITariTransportConfig {
        val cookieFile = File(torConfig.cookieFilePath)
        if (!cookieFile.exists()) {
            cookieFile.createNewFile()
        }
        val cookieString: ByteArray = cookieFile.readBytes()
        val torCookie = FFIByteVector(cookieString)
        return FFITariTransportConfig(
            controlAddress = NetAddressString(torConfig.controlHost, torConfig.controlPort),
            torCookie = torCookie,
            torPort = torConfig.connectionPort,
            socksUsername = torConfig.sock5Username,
            socksPassword = torConfig.sock5Password,
        )
    }

    /**
     * Starts the log file observer only in debug mode.
     * Will skip if the app is in release config.
     */
    private fun startLogFileObserver() {
        if (BuildConfig.DEBUG) {
            logFileObserver = LogFileObserver(walletConfig.getWalletLogFilePath())
            logFileObserver?.startWatching()
        }
    }

    /**
     * Stores wallet's Base58 address and emoji id into the shared prefs
     * for future convenience.
     */
    private fun saveWalletAddressToSharedPrefs() {
        // set shared preferences values after instantiation
        walletInstance?.getWalletAddress()?.let { ffiTariWalletAddress ->
            corePrefRepository.walletAddressBase58 = ffiTariWalletAddress.fullBase58()
            corePrefRepository.emojiId = ffiTariWalletAddress.getEmojiId()
            ffiTariWalletAddress.destroy()
        }
    }

    /**
     * Initializes the wallet and sets the singleton instance in the wallet companion object.
     */
    private fun initWallet() {
        if (walletInstance == null) {
            // store network info in shared preferences if it's a new wallet
            val isNewInstallation = !WalletUtil.walletExists(walletConfig)
            walletInstance = FFIWallet(
                sharedPrefsRepository = corePrefRepository,
                securityPrefRepository = securityPrefRepository,
                seedPhraseRepository = seedPhraseRepository,
                networkRepository = networkPrefRepository,
                commsConfig = getCommsConfig(),
                logPath = walletConfig.getWalletLogFilePath(),
                listener = object : FFIWalletListener {
                    /**
                     * All the callbacks are called on the FFI thread, so we need to switch to the main thread.
                     * The app will crash if we try to update the UI from the FFI thread.
                     */
                    override fun onTxReceived(pendingInboundTx: PendingInboundTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxReceived(pendingInboundTx))
                        postTxNotification(pendingInboundTx)
                    }

                    override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxReplyReceived(pendingOutboundTx))
                    }

                    override fun onTxFinalized(pendingInboundTx: PendingInboundTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxFinalized(pendingInboundTx))
                    }

                    override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.InboundTxBroadcast(pendingInboundTx))
                    }

                    override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.OutboundTxBroadcast(pendingOutboundTx))
                    }

                    override fun onTxMined(completedTx: CompletedTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxMined(completedTx))
                    }

                    override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxMinedUnconfirmed(completedTx, confirmationCount))
                    }

                    override fun onTxFauxConfirmed(completedTx: CompletedTx) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxFauxConfirmed(completedTx))
                    }

                    override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxFauxMinedUnconfirmed(completedTx, confirmationCount))
                    }

                    override fun onDirectSendResult(txId: BigInteger, status: TransactionSendStatus) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.DirectSendResult(TxId(txId), status))
                        outboundTxIdsToBePushNotified.firstOrNull { it.txId == txId }?.let {
                            outboundTxIdsToBePushNotified.remove(it)
                            sendPushNotificationToTxRecipient(it.recipientPublicKeyHex)
                        }
                    }

                    override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) = runOnMain {
                        _walletEvent.send(WalletEvent.Tx.TxCancelled(cancelledTx))

                        // TODO don't use android components in this class
                        val currentActivity = app.currentActivity
                        if (cancelledTx.direction == Tx.Direction.INBOUND
                            && !(app.isInForeground && currentActivity is HomeActivity && currentActivity.willNotifyAboutNewTx())
                        ) {
                            notificationHelper.postTxCanceledNotification(cancelledTx)
                        }
                    }

                    override fun onTXOValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) = runOnMain {
                        checkValidationResult(
                            type = WalletValidationType.TXO,
                            responseId = responseId,
                            isSuccess = status == TransactionValidationStatus.Success,
                        )
                    }

                    override fun onTxValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) = runOnMain {
                        checkValidationResult(
                            type = WalletValidationType.TX,
                            responseId = responseId,
                            isSuccess = status == TransactionValidationStatus.Success,
                        )
                        walletInstance
                            ?.takeIf { !txBroadcastRestarted && status == TransactionValidationStatus.Success }
                            ?.let { wallet ->
                                wallet.restartTxBroadcast()
                                txBroadcastRestarted = true
                            } ?: logger.i("Transaction broadcast restart failed because wallet instance is null or tx broadcast already restarted")
                    }

                    override fun onBalanceUpdated(balanceInfo: BalanceInfo) = runOnMain {
                        EventBus.balanceState.post(balanceInfo) // TODO replace with flow!!!
                    }

                    override fun onConnectivityStatus(status: Int) = runOnMain {
                        when (ConnectivityStatus.entries[status]) {
                            ConnectivityStatus.CONNECTING -> {
                                /* do nothing */
                            }

                            ConnectivityStatus.ONLINE -> {
                                walletInstance?.let { baseNodesManager.refreshBaseNodeList(it) }
                                    ?: logger.i("Wallet instance is null when trying to refresh base node list")
                                baseNodesManager.setBaseNodeState(BaseNodeState.Online)
                                EventBus.baseNodeState.post(BaseNodeState.Online)
                            }

                            ConnectivityStatus.OFFLINE -> {
                                val currentBaseNode = baseNodesManager.currentBaseNode
                                if (currentBaseNode == null || !currentBaseNode.isCustom) {
                                    baseNodesManager.setNextBaseNode()
                                    syncBaseNode()
                                }
                                baseNodesManager.setBaseNodeState(BaseNodeState.Offline)
                                EventBus.baseNodeState.post(BaseNodeState.Offline)
                            }
                        }
                    }

                    override fun onWalletRestoration(result: WalletRestorationResult) = runOnMain {
                        EventBus.walletRestorationState.post(result) // TODO replace with flow!!!
                    }

                    override fun onWalletScannedHeight(height: Int) = runOnMain {
                        baseNodesManager.saveWalletScannedHeight(height)
                    }

                    override fun onBaseNodeStateChanged(baseNodeState: FFITariBaseNodeState) = runOnMain {
                        baseNodesManager.saveBaseNodeState(baseNodeState)
                    }
                }
            )

            if (isNewInstallation) {
                walletInstance?.setKeyValue(
                    key = WalletService.Companion.KeyValueStorageKeys.NETWORK,
                    value = networkPrefRepository.currentNetwork.network.uriComponent,
                )
            } else if (tariSettingsPrefRepository.isRestoredWallet && networkPrefRepository.ffiNetwork == null) {
                networkPrefRepository.ffiNetwork = try {
                    Network.from(walletInstance?.getKeyValue(WalletService.Companion.KeyValueStorageKeys.NETWORK) ?: "")
                } catch (exception: Exception) {
                    null
                }
            }
            startLogFileObserver()

            walletInstance?.let { baseNodesManager.refreshBaseNodeList(it) }
                ?: error("Wallet instance is null when trying to refresh base node list")
            if (baseNodesManager.currentBaseNode == null) {
                baseNodesManager.setNextBaseNode()
            }
            syncBaseNode()
            saveWalletAddressToSharedPrefs()
        }
    }

    private fun postTxNotification(tx: Tx) {
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
        walletInstance?.let { wallet ->
            val senderHex = wallet.getWalletAddress().notificationHex()
            notificationService.notifyRecipient(recipientHex, senderHex, wallet::signMessage)
        } ?: logger.i("Wallet instance is null when trying to send push notification to recipient")
    }

    private fun checkValidationResult(type: WalletValidationType, responseId: BigInteger, isSuccess: Boolean) {
        try {
            val currentStatus = walletValidationStatusMap[type] ?: return
            if (currentStatus.requestKey != responseId) return
            walletValidationStatusMap[type] = WalletValidationResult(currentStatus.requestKey, isSuccess)
            logger.i("startSync:wallet validation:validation result: $type: $isSuccess")
            checkBaseNodeSyncCompletion()
        } catch (e: Throwable) {
            logger.i(e.toString())
        }
    }

    private fun checkBaseNodeSyncCompletion() {
        // make a copy of the status map for concurrency protection
        val statusMapCopy = walletValidationStatusMap.toMap()
        // if base node not in sync, then switch to the next base node
        // check if any has failed
        val failed = statusMapCopy.any { it.value.isSuccess == false }
        if (failed) {
            walletValidationStatusMap.clear()
            val currentBaseNode = baseNodesManager.currentBaseNode
            if (currentBaseNode == null || !currentBaseNode.isCustom) {
                baseNodesManager.setNextBaseNode()
                syncBaseNode()
            }
            EventBus.baseNodeSyncState.post(BaseNodeSyncState.Failed) // TODO replace with flow!!!
            return
        }
        // if any of the results is null, we're still waiting for all callbacks to happen
        val inProgress = statusMapCopy.any { it.value.isSuccess == null }
        if (inProgress) {
            return
        }
        // check if it's successful
        val successful = statusMapCopy.all { it.value.isSuccess == true }
        if (successful) {
            walletValidationStatusMap.clear()
            EventBus.baseNodeSyncState.post(BaseNodeSyncState.Online) // TODO replace with flow!!!
        }
        // shouldn't ever reach here - no-op
    }

    enum class ConnectivityStatus(val value: Int) {
        CONNECTING(0),
        ONLINE(1),
        OFFLINE(2),
    }

    data class OutboundTxNotification(val txId: BigInteger, val recipientPublicKeyHex: String)

    enum class WalletValidationType { TXO, TX }
    data class WalletValidationResult(val requestKey: BigInteger, val isSuccess: Boolean?)

    private fun runOnMain(block: suspend CoroutineScope.() -> Unit) {
        applicationScope.launch(Dispatchers.Main) { block() }
    }

    sealed class WalletEvent {
        object Tx {
            data class TxReceived(val tx: PendingInboundTx) : WalletEvent()
            data class TxReplyReceived(val tx: PendingOutboundTx) : WalletEvent()
            data class TxFinalized(val tx: PendingInboundTx) : WalletEvent()
            data class InboundTxBroadcast(val tx: PendingInboundTx) : WalletEvent()
            data class OutboundTxBroadcast(val tx: PendingOutboundTx) : WalletEvent()
            data class TxMined(val tx: CompletedTx) : WalletEvent()
            data class TxMinedUnconfirmed(val tx: CompletedTx, val confirmationCount: Int) : WalletEvent()
            data class TxFauxConfirmed(val tx: CompletedTx) : WalletEvent()
            data class TxFauxMinedUnconfirmed(val tx: CompletedTx, val confirmationCount: Int) : WalletEvent()
            data class TxCancelled(val tx: CancelledTx) : WalletEvent()
            data class DirectSendResult(val txId: TxId, val status: TransactionSendStatus) : WalletEvent()
        }
    }
}

interface OutboundTxNotifier {
    val outboundTxIdsToBePushNotified: CopyOnWriteArraySet<WalletManager.OutboundTxNotification>
}
