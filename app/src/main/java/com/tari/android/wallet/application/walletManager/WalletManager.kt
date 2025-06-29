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

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.AppStateHandler
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.application.walletManager.WalletCallbacks.Companion.MAIN_WALLET_CONTEXT_ID
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.data.recovery.WalletRestorationStateHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.ffi.Base58String
import com.tari.android.wallet.ffi.FFIByteVector
import com.tari.android.wallet.ffi.FFICommsConfig
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ffi.FFISeedWords
import com.tari.android.wallet.ffi.FFITariTransportConfig
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.ffi.NetAddressString
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TransactionSendStatus
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.fullBase58
import com.tari.android.wallet.model.seedPhrase.SeedPhrase
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingInboundTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.notification.FcmHelper
import com.tari.android.wallet.tor.TorConfig
import com.tari.android.wallet.tor.TorProxyManager
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.ui.common.DialogManager
import com.tari.android.wallet.ui.screen.send.obsolete.finalize.FinalizeSendTxModel
import com.tari.android.wallet.util.BroadcastEffectFlow
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.safeCastTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigInteger
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
    private val walletRestorationStateHandler: WalletRestorationStateHandler,
    private val dialogManager: DialogManager,
    private val balanceStateHandler: BalanceStateHandler,
    private val walletCallbacks: WalletCallbacks,
    private val appStateHandler: AppStateHandler,
    private val fcmHelper: FcmHelper,
    private val airdropRepository: AirdropRepository,
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

    private val _walletEvent = BroadcastEffectFlow<WalletEvent>()
    val walletEvent: Flow<WalletEvent> = _walletEvent.flow

    private val _txSentConfirmations = MutableStateFlow(emptyList<TxSendResult>())
    val txSentConfirmations = _txSentConfirmations.asStateFlow()

    private val logger
        get() = Logger.t(WalletManager::class.simpleName)

    private val walletValidator = WalletValidator(
        walletManager = this,
        baseNodeStateHandler = baseNodeStateHandler,
        baseNodesManager = baseNodesManager,
    )

    init {
        applicationScope.collectFlow(appStateHandler.appEvent) { event ->
            when (event) {
                is AppStateHandler.AppEvent.AppBackgrounded,
                is AppStateHandler.AppEvent.AppForegrounded,
                is AppStateHandler.AppEvent.AppDestroyed -> walletConfig.removeUnnecessaryLogs()
            }
        }

        applicationScope.collectFlow(airdropRepository.airdropToken) { airdropToken ->
            if (airdropToken != null) {
                doOnWalletRunning { fcmHelper.getFcmTokenAndRegister(it) }
            }
        }
    }

    // ------------------------------------------------------ Start Wallet ------------------------------------------------------

    // TODO Use the startBalance properly
    @Synchronized
    fun start(seedWords: List<String>? = null, startBalance: String? = null) {
        val ffiSeedWords = SeedPhrase.createOrNull(seedWords)

        walletCallbacks.addListener(
            walletContextId = MAIN_WALLET_CONTEXT_ID,
            listener = MainFFIWalletListener(
                walletManager = this,
                walletValidator = walletValidator,
                externalScope = applicationScope,
                baseNodesManager = baseNodesManager,
                balanceStateHandler = balanceStateHandler,
                baseNodeStateHandler = baseNodeStateHandler,
                walletRestorationStateHandler = walletRestorationStateHandler,
            ),
        )

        torManager.run()
        applicationScope.launch {
            torProxyStateHandler.doOnTorReadyForWallet {
                startWallet(ffiSeedWords, startBalance)
            }
        }
    }

    private fun startWallet(ffiSeedWords: FFISeedWords?, startBalance: String?) {
        if (walletState.value is WalletState.NotReady || walletState.value is WalletState.Failed) {
            logger.i("Start wallet: Initializing wallet...")
            _walletState.update { WalletState.Initializing }
            applicationScope.launch {
                try {
                    if (walletInstance == null) {
                        walletInstance = initWallet(ffiSeedWords)
                    }
                    _walletState.update { WalletState.Running }
                    logger.i("Start wallet: Wallet was started")
                } catch (e: Exception) {
                    val oldCode = walletState.value.errorCode
                    val newCode = e.safeCastTo<FFIException>()?.error?.code

                    if (oldCode == null || oldCode != newCode) {
                        logger.i("Start wallet: Error starting wallet: $newCode")
                        _walletState.update { WalletState.Failed(e) }
                    }
                }
            }
        }
    }

    private fun initWallet(ffiSeedWords: FFISeedWords?): FFIWallet {
        val passphrase = securityPrefRepository.databasePassphrase.takeIf { !it.isNullOrEmpty() }
            ?: corePrefRepository.generateDatabasePassphrase().also { securityPrefRepository.databasePassphrase = it }

        val wallet = FFIWallet(
            walletContextId = MAIN_WALLET_CONTEXT_ID,
            tariNetwork = networkPrefRepository.currentNetwork,
            commsConfig = createCommsConfig(),
            logPath = walletConfig.getWalletLogFilePath(),
            passphrase = passphrase,
            seedWords = ffiSeedWords,
            walletCallbacks = walletCallbacks,
        )

        if (DebugConfig.selectBaseNodeEnabled) {
            baseNodesManager.refreshBaseNodeList(wallet)
            if (baseNodesManager.currentBaseNode == null) {
                baseNodesManager.setNextBaseNode()
            }
        }

        // Need to update the balance state after the wallet is initialized,
        // because the first balance callback is called after the wallet is connected to the base node and validated
        balanceStateHandler.updateBalanceState(wallet.getBalance())

        applicationScope.launch(Dispatchers.IO) {
            baseNodeStateHandler.doOnBaseNodeOnline {
                walletValidator.validateWallet()
            }
        }

        saveWalletAddressToSharedPrefs(wallet)

        // register wallet for push notifications
        fcmHelper.getFcmTokenAndRegister(wallet)

        return wallet
    }

    private fun createCommsConfig(): FFICommsConfig = FFICommsConfig(
        publicAddress = NetAddressString(address = "127.0.0.1", port = 39069).toString(),
        transport = createTorTransportConfig(),
        databaseName = WalletConfig.WALLET_DB_NAME,
        datastorePath = walletConfig.getWalletFilesDirPath(),
        discoveryTimeoutSec = Constants.Wallet.DISCOVERY_TIMEOUT_SEC,
        safMessageDurationSec = Constants.Wallet.STORE_AND_FORWARD_MESSAGE_DURATION_SEC,
    )

    private fun createTorTransportConfig(): FFITariTransportConfig {
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
     * Stores wallet's Base58 address and emoji id into the shared prefs
     * for future convenience.
     */
    private fun saveWalletAddressToSharedPrefs(wallet: FFIWallet) {
        wallet.getWalletAddress().runWithDestroy { ffiTariWalletAddress ->
            corePrefRepository.walletAddressBase58 = ffiTariWalletAddress.fullBase58()
            corePrefRepository.emojiId = ffiTariWalletAddress.getEmojiId()
        }
    }

    // ------------------------------------------------------ Sync Base Node ------------------------------------------------------

    /**
     * Syncs the wallet with the base node and validates the wallet
     */
    @Deprecated("Should be removed once the BaseNode pinning feature is implemented")
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

                            walletValidator.validateWallet()
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

    // ------------------------------------------------------ Restore Wallet ------------------------------------------------------

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
            return walletInstance?.startRecovery(recoveryOutputMessage) == true
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

    // ------------------------------------------------------ Stop Wallet ------------------------------------------------------

    @Synchronized
    fun stop() {
        walletInstance?.destroy()
        walletInstance = null
        _walletState.update { WalletState.NotReady }
        // stop tor proxy
        torManager.shutdown()
        walletCallbacks.removeListener(MAIN_WALLET_CONTEXT_ID)
    }

    fun deleteWallet() {
        logger.i("Deleting wallet: ${walletInstance?.getWalletAddress()?.fullBase58() ?: "wallet is already null!"}")
        walletInstance?.destroy()
        walletInstance = null
        _walletState.update { WalletState.NotReady }
        applicationScope.launch(Dispatchers.Main) {
            _walletEvent.send(WalletEvent.OnWalletRemove)
            dialogManager.dismissAll()
        }
        walletConfig.clearWalletFiles()
        corePrefRepository.clear()
        walletCallbacks.removeAllListeners()
    }

    // ------------------------------------------------------ Misc ------------------------------------------------------

    fun sendWalletEvent(event: WalletEvent) {
        applicationScope.launch {
            _walletEvent.send(event)
        }
    }

    fun updateTxSentConfirmations(txSendResult: TxSendResult) {
        _txSentConfirmations.update { it + txSendResult }
    }

    @Throws(FFIException::class)
    fun sendTari(
        tariContact: TariContact,
        amount: MicroTari,
        feePerGram: MicroTari,
        message: String,
        isOneSidePayment: Boolean,
    ): TxId {
        val recipientAddress = FFITariWalletAddress(Base58String(tariContact.walletAddress.fullBase58))

        val txId = requireWalletInstance.sendTx(recipientAddress, amount.value, feePerGram.value, message, isOneSidePayment)

        recipientAddress.destroy()
        return txId
    }

    fun getLastAccessedToDbVersion(): String {
        return createCommsConfig().runWithDestroy { it.getLastVersion() }
    }

    enum class ConnectivityStatus(val value: Int) {
        CONNECTING(0),
        ONLINE(1),
        OFFLINE(2),
    }

    enum class WalletValidationType { TXO, TX }
    data class WalletValidationResult(val requestKey: BigInteger, val isSuccess: Boolean?)

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
