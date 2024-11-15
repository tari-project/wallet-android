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
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.application.walletManager.WalletCallbackListener.Companion.MAIN_WALLET_CONTEXT_ID
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFICommsConfig
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFISeedWords
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import com.tari.android.wallet.ffi.FFITariTransportConfig
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ffi.LogFileObserver
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.ffi.TransactionValidationStatus
import com.tari.android.wallet.ffi.runWithDestroy
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
import com.tari.android.wallet.model.fullBase58
import com.tari.android.wallet.model.seedPhrase.SeedPhrase
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.recovery.WalletRestorationState
import com.tari.android.wallet.recovery.WalletRestorationStateHandler
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.ui.common.DialogManager
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.send.finalize.FinalizeSendTxModel
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
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
    private val networkPrefRepository: NetworkPrefRepository,
    private val tariSettingsPrefRepository: TariSettingsPrefRepository,
    private val securityPrefRepository: SecurityPrefRepository,
    private val baseNodesManager: BaseNodesManager,
    private val torConfig: TorConfig,
    private val torProxyStateHandler: TorProxyStateHandler,
    private val baseNodeStateHandler: BaseNodeStateHandler,
    private val app: TariWalletApplication,
    private val notificationHelper: NotificationHelper,
    private val walletRestorationStateHandler: WalletRestorationStateHandler,
    private val walletServiceLauncher: WalletServiceLauncher,
    private val dialogManager: DialogManager,
    private val balanceStateHandler: BalanceStateHandler,
    private val walletCallbackListener: WalletCallbackListener,
    private val walletNotificationManager: WalletNotificationManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private var atomicInstance = AtomicReference<FFIWallet>()
    var walletInstance: FFIWallet?
        get() = atomicInstance.get()
        set(value) = atomicInstance.set(value)
    val requireWalletInstance: FFIWallet
        get() = walletInstance ?: error("Wallet instance is null")

    private val _walletState = MutableStateFlow<WalletState>(WalletState.NotReady)
    val walletState = _walletState.asStateFlow()

    private val _walletEvent = EffectChannelFlow<WalletEvent>()
    val walletEvent: Flow<WalletEvent> = _walletEvent.flow

    private val _txSentConfirmations = MutableStateFlow(emptyList<TxSendResult>())
    val txSentConfirmations = _txSentConfirmations.asStateFlow()

    private var logFileObserver: LogFileObserver? = null
    private val logger
        get() = Logger.t(WalletManager::class.simpleName)

    /**
     * Maps the validation type to the request id and validation result. This map will be
     * initialized at the beginning of each base node validation sequence.
     * Validation results will all be null, and will be set as the result callbacks get called.
     */
    private val walletValidationStatusMap: ConcurrentMap<WalletValidationType, WalletValidationResult> = ConcurrentHashMap()

    private var txBroadcastRestarted = false

    @Synchronized
    fun start(seedWords: List<String>?) {
        torManager.run()

        val ffiSeedWords = SeedPhrase.createOrNull(seedWords)

        applicationScope.launch {
            torProxyStateHandler.doOnTorReadyForWallet {
                startWallet(ffiSeedWords)
            }
        }

        walletCallbackListener.addListener(
            walletContextId = MAIN_WALLET_CONTEXT_ID,
            listener = object : FFIWalletListener {
                /**
                 * All the callbacks are called on the FFI thread, so we need to switch to the main thread.
                 * The app will crash if we try to update the UI from the FFI thread.
                 */
                override fun onTxReceived(pendingInboundTx: PendingInboundTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxReceived(
                            tx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress)),
                        )
                    )
                    walletNotificationManager.postTxNotification(pendingInboundTx)
                }

                override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxReplyReceived(
                            tx = pendingOutboundTx.copy(tariContact = getUserByWalletAddress(pendingOutboundTx.tariContact.walletAddress)),
                        )
                    )
                }

                override fun onTxFinalized(pendingInboundTx: PendingInboundTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxFinalized(
                            tx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress)),
                        )
                    )
                }

                override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.InboundTxBroadcast(
                            tx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress)),
                        )
                    )
                }

                override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.OutboundTxBroadcast(
                            tx = pendingOutboundTx.copy(tariContact = getUserByWalletAddress(pendingOutboundTx.tariContact.walletAddress)),
                        )
                    )
                }

                override fun onTxMined(completedTx: CompletedTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxMined(
                            tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
                        )
                    )
                }

                override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxMinedUnconfirmed(
                            tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
                            confirmationCount = confirmationCount,
                        )
                    )
                }

                override fun onTxFauxConfirmed(completedTx: CompletedTx) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxFauxConfirmed(
                            tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
                        )
                    )
                }

                override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxFauxMinedUnconfirmed(
                            tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
                            confirmationCount = confirmationCount,
                        )
                    )
                }

                override fun onDirectSendResult(txId: BigInteger, status: TransactionSendStatus) = runOnMain {
                    _txSentConfirmations.update { it + TxSendResult(TxId(txId), status) }
                    walletNotificationManager.sendOutboundTxNotification(requireWalletInstance, txId, status)
                }

                override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) = runOnMain {
                    _walletEvent.send(
                        WalletEvent.Tx.TxCancelled(
                            tx = cancelledTx.copy(tariContact = getUserByWalletAddress(cancelledTx.tariContact.walletAddress)),
                        )
                    )

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
                    walletInstance?.let {
                        if (!txBroadcastRestarted && status == TransactionValidationStatus.Success) {
                            it.restartTxBroadcast()
                            txBroadcastRestarted = true
                            logger.i("Wallet validation: Transaction broadcast restarted (requestId: $responseId)")
                        }
                    }
                        ?: logger.i("Wallet validation: error: Transaction broadcast restart failed because wallet instance is null (requestId: $responseId)\"")
                }

                override fun onBalanceUpdated(balanceInfo: BalanceInfo) = runOnMain {
                    balanceStateHandler.updateBalanceState(balanceInfo)
                }

                override fun onConnectivityStatus(status: Int) = runOnMain {
                    when (ConnectivityStatus.entries[status]) {
                        ConnectivityStatus.CONNECTING -> {
                            logger.i("Base Node connection: connecting...")
                        }

                        ConnectivityStatus.ONLINE -> {
                            if (DebugConfig.selectBaseNodeEnabled) baseNodesManager.refreshBaseNodeList(requireWalletInstance)
                            if (baseNodeStateHandler.updateState(BaseNodeState.Online)) {
                                logger.i("Base Node connection: connected [ONLINE]")
                            }
                        }

                        ConnectivityStatus.OFFLINE -> {
                            val currentBaseNode = baseNodesManager.currentBaseNode
                            if (DebugConfig.selectBaseNodeEnabled && (currentBaseNode == null || !currentBaseNode.isCustom)) {
                                baseNodesManager.setNextBaseNode()
                                syncBaseNode()
                            }
                            if (baseNodeStateHandler.updateState(BaseNodeState.Offline)) {
                                logger.i("Base Node connection: disconnected [OFFLINE]")
                            }
                        }
                    }
                }

                override fun onWalletRestoration(state: WalletRestorationState) = runOnMain {
                    walletRestorationStateHandler.updateState(state)
                }

                override fun onWalletScannedHeight(height: Int) = runOnMain {
                    baseNodesManager.saveWalletScannedHeight(height)
                }

                override fun onBaseNodeStateChanged(baseNodeState: FFITariBaseNodeState) = runOnMain {
                    baseNodesManager.saveBaseNodeState(baseNodeState)
                }
            },
        )
    }

    @Synchronized
    fun stop() {
        // destroy FFI wallet object
        walletInstance?.destroy()
        walletInstance = null
        _walletState.update { WalletState.NotReady }
        // stop tor proxy
        torManager.shutdown()
        walletCallbackListener.removeListener(MAIN_WALLET_CONTEXT_ID)
    }

    fun onWalletStarted() {
        _walletState.update { WalletState.Running }
    }

    fun getCommsConfig(): FFICommsConfig = FFICommsConfig(
        publicAddress = NetAddressString(address = "127.0.0.1", port = 39069).toString(),
        transport = getTorTransport(),
        databaseName = WalletConfig.WALLET_DB_NAME,
        datastorePath = walletConfig.getWalletFilesDirPath(),
        discoveryTimeoutSec = Constants.Wallet.DISCOVERY_TIMEOUT_SEC,
        safMessageDurationSec = Constants.Wallet.STORE_AND_FORWARD_MESSAGE_DURATION_SEC,
    )

    /**
     * Syncs the wallet with the base node and validates the wallet
     */
    fun syncBaseNode() {
        if (!DebugConfig.selectBaseNodeEnabled) {
            Logger.e("Base Node connection: Base node selection is disabled, but syncBaseNode() is called!!")
        }
        var currentBaseNode: BaseNodeDto? = baseNodesManager.currentBaseNode ?: return

        applicationScope.launch(Dispatchers.IO) {
            doOnWalletRunning { wallet ->
                while (currentBaseNode != null) {
                    try {
                        currentBaseNode?.let { it ->
                            logger.i("Base Node connection: sync with base node ${it.publicKeyHex}::${it.address} started")
                            val baseNodeKeyFFI = FFIPublicKey(HexString(it.publicKeyHex))
                            val addBaseNodeResult = wallet.addBaseNodePeer(baseNodeKeyFFI, it.address)
                            baseNodeKeyFFI.destroy()
                            logger.i("Base Node connection: addBaseNodePeer ${if (addBaseNodeResult) "success" else "failed"}")

                            validateWallet()
                        }
                        break
                    } catch (e: Throwable) {
                        logger.i("Base Node connection: error connecting to base node $currentBaseNode with an error: ${e.message}")
                        currentBaseNode = baseNodesManager.setNextBaseNode()
                    }
                }

                if (currentBaseNode == null) {
                    logger.e("Base Node connection: error: cannot connect to any base node")
                }
            }
        }
    }

    /**
     * Starts the wallet recovery process. Returns true if the recovery process was started successfully.
     * The recovery process events will be handled in the onWalletRestoration() callback.
     */
    fun startRecovery(baseNode: BaseNodeDto?, recoveryOutputMessage: String): Boolean {
        if (DebugConfig.selectBaseNodeEnabled) {
            // TODO we don't support selecting base node for recovery yet
            //  val baseNodeFFI = baseNode?.let { FFIPublicKey(HexString(it.publicKeyHex)) }
            //  return walletInstance?.startRecovery(baseNodeFFI, recoveryOutputMessage) ?: false
            return false
        } else {
            return walletInstance?.startRecovery(recoveryOutputMessage) ?: false
        }
    }

    fun onWalletRestored() {
        corePrefRepository.onboardingCompleted = true
        corePrefRepository.onboardingStarted = true
        corePrefRepository.onboardingAuthSetupStarted = true
        corePrefRepository.onboardingAuthSetupCompleted = false
        corePrefRepository.onboardingDisplayedAtHome = true
        corePrefRepository.needToShowRecoverySuccessDialog = true
        tariSettingsPrefRepository.isRestoredWallet = true
    }

    fun deleteWallet() {
        logger.i("Deleting wallet: ${walletInstance?.getWalletAddress()?.fullBase58() ?: "wallet is already null!"}")
        walletInstance?.destroy()
        walletInstance = null
        _walletState.update { WalletState.NotReady }
        runOnMain { _walletEvent.send(WalletEvent.OnWalletRemove) }
        walletConfig.clearWalletFiles()
        corePrefRepository.clear()
        dialogManager.dismissAll()
        walletServiceLauncher.stop()
        walletCallbackListener.removeAllListeners()
    }

    fun sendWalletEvent(event: WalletEvent) {
        applicationScope.launch {
            _walletEvent.send(event)
        }
    }

    private fun startWallet(ffiSeedWords: FFISeedWords?) {
        if (walletState.value is WalletState.NotReady || walletState.value is WalletState.Failed) {
            logger.i("Start wallet: Initializing wallet...")
            _walletState.update { WalletState.Initializing }
            applicationScope.launch {
                try {
                    initWallet(ffiSeedWords)
                    _walletState.update { WalletState.Started }
                    logger.i("Start wallet: Wallet was started")
                } catch (e: Exception) {
                    val oldCode = walletState.value.errorCode
                    val newCode = e.safeCastTo<FFIException>()?.error?.code

                    if (oldCode == null || oldCode != newCode) {
                        logger.e(e, "Start wallet: Wallet was failed")
                    }
                    _walletState.update { WalletState.Failed(e) }
                }
            }
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
    private fun saveWalletAddressToSharedPrefs(wallet: FFIWallet) {
        wallet.getWalletAddress().runWithDestroy { ffiTariWalletAddress ->
            corePrefRepository.walletAddressBase58 = ffiTariWalletAddress.fullBase58()
            corePrefRepository.emojiId = ffiTariWalletAddress.getEmojiId()
        }
    }

    /**
     * Initializes the wallet and sets the singleton instance in the wallet companion object.
     */
    private fun initWallet(ffiSeedWords: FFISeedWords?) {
        if (walletInstance == null) {
            val passphrase = securityPrefRepository.databasePassphrase.takeIf { !it.isNullOrEmpty() }
                ?: corePrefRepository.generateDatabasePassphrase().also { securityPrefRepository.databasePassphrase = it }

            walletInstance = FFIWallet(
                walletContextId = MAIN_WALLET_CONTEXT_ID,
                tariNetwork = networkPrefRepository.currentNetwork,
                commsConfig = getCommsConfig(),
                logPath = walletConfig.getWalletLogFilePath(),
                passphrase = passphrase,
                seedWords = ffiSeedWords,
                walletCallbackListener = walletCallbackListener,
            )

            startLogFileObserver()

            if (DebugConfig.selectBaseNodeEnabled) {
                baseNodesManager.refreshBaseNodeList(requireWalletInstance)
                if (baseNodesManager.currentBaseNode == null) {
                    baseNodesManager.setNextBaseNode()
                }
            }

            // Need to update the balance state after the wallet is initialized,
            // because the first balance callback is called after the wallet is connected to the base node and validated
            balanceStateHandler.updateBalanceState(requireWalletInstance.getBalance())

            applicationScope.launch(Dispatchers.IO) {
                baseNodeStateHandler.doOnBaseNodeOnline {
                    validateWallet()
                }
            }

            saveWalletAddressToSharedPrefs(requireWalletInstance)
        }
    }

    private suspend fun validateWallet() {
        doOnWalletRunning { wallet ->
            try {
                logger.i("Wallet validation: Starting Tx and TXO validation...")
                walletValidationStatusMap.clear()
                walletValidationStatusMap[WalletValidationType.TXO] = WalletValidationResult(wallet.startTXOValidation(), null)
                walletValidationStatusMap[WalletValidationType.TX] = WalletValidationResult(wallet.startTxValidation(), null)
                logger.i(
                    "Wallet validation: Started Tx and TXO validation with " +
                            "request keys: ${Gson().toJson(walletValidationStatusMap.map { it.value.requestKey })}"
                )
            } catch (e: Throwable) {
                logger.i("Wallet validation: Error: ${e.message}")
                walletValidationStatusMap.clear()
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Failed)
            }
        }
    }

    private fun checkValidationResult(type: WalletValidationType, responseId: BigInteger, isSuccess: Boolean) {
        try {
            val currentStatus = walletValidationStatusMap[type] ?: return
            if (currentStatus.requestKey != responseId) return
            walletValidationStatusMap[type] = WalletValidationResult(currentStatus.requestKey, isSuccess)
            logger.i("Wallet validation: Validation result for request $responseId: $type: ${if (isSuccess) "Success" else "Failed!"}")
            checkBaseNodeSyncCompletion()
        } catch (e: Throwable) {
            logger.i("Wallet validation: $type validation for request $responseId failed with an error: ${e.message}")
        }
    }

    private fun checkBaseNodeSyncCompletion() {
        // make a copy of the status map for concurrency protection
        val statusMapCopy = walletValidationStatusMap.toMap()

        val failed = statusMapCopy.any { it.value.isSuccess == false }
        val inProgress = statusMapCopy.any { it.value.isSuccess == null }
        val successful = statusMapCopy.all { it.value.isSuccess == true }

        when {
            failed -> {
                walletValidationStatusMap.clear()
                val currentBaseNode = baseNodesManager.currentBaseNode
                if (DebugConfig.selectBaseNodeEnabled && (currentBaseNode == null || !currentBaseNode.isCustom)) {
                    baseNodesManager.setNextBaseNode()
                    syncBaseNode()
                }
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Failed)
            }

            inProgress -> {
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Syncing)
            }

            successful -> {
                walletValidationStatusMap.clear()
                baseNodeStateHandler.updateSyncState(BaseNodeSyncState.Online)
                logger.i("Wallet validation: Validation completed successfully")
            }
        }
    }

    private fun getUserByWalletAddress(address: TariWalletAddress): TariContact =
        requireWalletInstance.findContactByWalletAddress(address)?.runWithDestroy { TariContact(it) } ?: TariContact(address)

    enum class ConnectivityStatus(val value: Int) {
        CONNECTING(0),
        ONLINE(1),
        OFFLINE(2),
    }

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
        }

        object TxSend {
            data class TxSendSuccessful(val txId: TxId) : WalletEvent()
            data class TxSendFailed(val failureReason: FinalizeSendTxModel.TxFailureReason) : WalletEvent()
        }

        data object OnWalletRemove : WalletEvent()

        data object UtxosSplit : WalletEvent()
    }

    data class TxSendResult(val txId: TxId, val status: TransactionSendStatus)
}
