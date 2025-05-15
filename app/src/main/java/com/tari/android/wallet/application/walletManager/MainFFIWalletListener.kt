package com.tari.android.wallet.application.walletManager

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.application.walletManager.WalletManager.ConnectivityStatus
import com.tari.android.wallet.application.walletManager.WalletManager.TxSendResult
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.application.walletManager.WalletManager.WalletValidationType
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.baseNode.BaseNodeState
import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.data.recovery.WalletRestorationState
import com.tari.android.wallet.data.recovery.WalletRestorationStateHandler
import com.tari.android.wallet.ffi.TransactionValidationStatus
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.TariBaseNodeState
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TransactionSendStatus
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingInboundTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger

/**
 * Main thread FFI wallet listener.
 * Since we have an option to have multiple wallet instances, we need to make sure that the callbacks are handled for the correct wallet instance.
 */
class MainFFIWalletListener(
    private val walletManager: WalletManager,
    private val walletValidator: WalletValidator,
    private val externalScope: CoroutineScope,
    private val baseNodesManager: BaseNodesManager,
    private val balanceStateHandler: BalanceStateHandler,
    private val baseNodeStateHandler: BaseNodeStateHandler,
    private val walletRestorationStateHandler: WalletRestorationStateHandler,
) : FFIWalletListener {

    private val logger
        get() = Logger.t(WalletManager::class.simpleName)

    private var txBroadcastRestarted = false

    /**
     * All the callbacks are called on the FFI thread, so we need to switch to the main thread.
     * The app will crash if we try to update the UI from the FFI thread.
     */
    override fun onTxReceived(pendingInboundTx: PendingInboundTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxReceived(
                tx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxReplyReceived(
                tx = pendingOutboundTx.copy(tariContact = getUserByWalletAddress(pendingOutboundTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onTxFinalized(pendingInboundTx: PendingInboundTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxFinalized(
                tx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.InboundTxBroadcast(
                tx = pendingInboundTx.copy(tariContact = getUserByWalletAddress(pendingInboundTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.OutboundTxBroadcast(
                tx = pendingOutboundTx.copy(tariContact = getUserByWalletAddress(pendingOutboundTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onTxMined(completedTx: CompletedTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxMined(
                tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxMinedUnconfirmed(
                tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
                confirmationCount = confirmationCount,
            )
        )
    }

    override fun onTxFauxConfirmed(completedTx: CompletedTx) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxFauxConfirmed(
                tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxFauxMinedUnconfirmed(
                tx = completedTx.copy(tariContact = getUserByWalletAddress(completedTx.tariContact.walletAddress)),
                confirmationCount = confirmationCount,
            )
        )
    }

    override fun onDirectSendResult(txId: TxId, status: TransactionSendStatus) = runOnMain {
        walletManager.updateTxSentConfirmations(TxSendResult(txId, status))
    }

    override fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) = runOnMain {
        walletManager.sendWalletEvent(
            WalletEvent.Tx.TxCancelled(
                tx = cancelledTx.copy(tariContact = getUserByWalletAddress(cancelledTx.tariContact.walletAddress)),
            )
        )
    }

    override fun onTXOValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) = runOnMain {
        walletValidator.checkValidationResult(
            type = WalletValidationType.TXO,
            responseId = responseId,
            isSuccess = status == TransactionValidationStatus.Success,
        )
    }

    override fun onTxValidationComplete(responseId: BigInteger, status: TransactionValidationStatus) = runOnMain {
        walletValidator.checkValidationResult(
            type = WalletValidationType.TX,
            responseId = responseId,
            isSuccess = status == TransactionValidationStatus.Success,
        )
        walletManager.walletInstance?.let {
            if (!txBroadcastRestarted && status == TransactionValidationStatus.Success) {
                it.restartTxBroadcast()
                txBroadcastRestarted = true
                logger.i("Wallet validation: Transaction broadcast restarted (requestId: $responseId)")
            }
        } ?: logger.i("Wallet validation: error: Transaction broadcast restart failed because wallet instance is null (requestId: $responseId)\"")
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
                if (DebugConfig.selectBaseNodeEnabled) baseNodesManager.refreshBaseNodeList(walletManager.requireWalletInstance)
                if (baseNodeStateHandler.updateState(BaseNodeState.Online)) {
                    logger.i("Base Node connection: connected [ONLINE]")
                }
            }

            ConnectivityStatus.OFFLINE -> {
                val currentBaseNode = baseNodesManager.currentBaseNode
                if (DebugConfig.selectBaseNodeEnabled && (currentBaseNode == null || !currentBaseNode.isCustom)) {
                    baseNodesManager.setNextBaseNode()
                    walletManager.syncBaseNode()
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

    override fun onBaseNodeStateChanged(baseNodeState: TariBaseNodeState) = runOnMain {
        baseNodesManager.saveBaseNodeState(baseNodeState)
    }

    private fun getUserByWalletAddress(address: TariWalletAddress): TariContact =
        walletManager.requireWalletInstance.findContactByWalletAddress(address)?.runWithDestroy { TariContact(it) } ?: TariContact(address)

    private fun runOnMain(block: suspend CoroutineScope.() -> Unit) {
        externalScope.launch(Dispatchers.Main) { block() }
    }
}