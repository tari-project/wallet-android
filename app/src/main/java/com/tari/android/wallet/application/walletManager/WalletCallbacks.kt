package com.tari.android.wallet.application.walletManager

import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.recovery.WalletRestorationState
import com.tari.android.wallet.ffi.FFIBalance
import com.tari.android.wallet.ffi.FFICompletedTx
import com.tari.android.wallet.ffi.FFIPendingInboundTx
import com.tari.android.wallet.ffi.FFIPointer
import com.tari.android.wallet.ffi.FFITariBaseNodeState
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.TariBaseNodeState
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingInboundTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.model.tx.Tx
import java.math.BigInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wallet callback listener. It's needed because of FFI specific callback handling.
 * We support multiple wallet instances, but from the FFI side, we can only have one static callback listener.
 *
 * !! Should be added to proguard rules since method names are used in FFI callbacks. !!
 */
@Singleton
class WalletCallbacks @Inject constructor() {

    private val logger
        get() = Logger.t(WalletCallbacks::class.simpleName)

    private val listeners = mutableMapOf<Int, FFIWalletListener>()

    fun addListener(walletContextId: Int, listener: FFIWalletListener) {
        listeners[walletContextId] = listener
    }

    fun removeListener(walletContextId: Int) {
        listeners.remove(walletContextId)
    }

    fun removeAllListeners() {
        listeners.clear()
    }

    fun onTxReceived(contextPtr: ByteArray, pendingInboundTxPtr: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val tx = FFIPendingInboundTx(pendingInboundTxPtr)
        log(walletContextId, "Tx received ${tx.getId()}")
        val pendingTx = PendingInboundTx(tx)
        listeners[walletContextId]?.onTxReceived(pendingTx)
    }

    fun onTxReplyReceived(contextPtr: ByteArray, txPointer: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val tx = FFICompletedTx(txPointer)
        log(walletContextId, "Tx reply received ${tx.getId()}")
        val pendingOutboundTx = PendingOutboundTx(tx)
        listeners[walletContextId]?.onTxReplyReceived(pendingOutboundTx)
    }

    fun onTxFinalized(contextPtr: ByteArray, completedTx: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val tx = FFICompletedTx(completedTx)
        log(walletContextId, "Tx finalized ${tx.getId()}")
        val pendingInboundTx = PendingInboundTx(tx)
        listeners[walletContextId]?.onTxFinalized(pendingInboundTx)
    }

    fun onTxBroadcast(contextPtr: ByteArray, completedTxPtr: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val tx = FFICompletedTx(completedTxPtr)
        when (tx.getDirection()) {
            Tx.Direction.INBOUND -> {
                val pendingInboundTx = PendingInboundTx(tx)
                log(walletContextId, "Tx inbound broadcast ${tx.getId()}")
                listeners[walletContextId]?.onInboundTxBroadcast(pendingInboundTx)
            }

            Tx.Direction.OUTBOUND -> {
                val pendingOutboundTx = PendingOutboundTx(tx)
                log(walletContextId, "Tx outbound broadcast ${tx.getId()}")
                listeners[walletContextId]?.onOutboundTxBroadcast(pendingOutboundTx)
            }
        }
    }

    fun onTxMined(contextPtr: ByteArray, completedTxPtr: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val completed = CompletedTx(completedTxPtr)
        log(walletContextId, "Tx mined & confirmed ${completed.id}")
        listeners[walletContextId]?.onTxMined(completed)
    }

    fun onTxMinedUnconfirmed(contextPtr: ByteArray, completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        log(walletContextId, "Tx mined & unconfirmed ${completed.id} ($confirmationCount confirmations)")
        listeners[walletContextId]?.onTxMinedUnconfirmed(completed, confirmationCount)
    }

    fun onTxFauxConfirmed(contextPtr: ByteArray, completedTxPtr: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val completed = CompletedTx(completedTxPtr)
        log(walletContextId, "Tx faux confirmed ${completed.id}")
        listeners[walletContextId]?.onTxMined(completed)
    }

    fun onTxFauxUnconfirmed(contextPtr: ByteArray, completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        log(walletContextId, "Tx faux unconfirmed ${completed.id} ($confirmationCount confirmations)")
        listeners[walletContextId]?.onTxMinedUnconfirmed(completed, confirmationCount)
    }

    fun onDirectSendResult(contextPtr: ByteArray, bytes: ByteArray, pointer: FFIPointer) {
        // FIXME: not used anymore, should be removed once FFI is updated
    }

    fun onTxCancelled(contextPtr: ByteArray, completedTx: FFIPointer, rejectionReason: ByteArray) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val rejectionReasonInt = BigInteger(1, rejectionReason).toInt()
        val tx = FFICompletedTx(completedTx)
        log(walletContextId, "Tx cancelled ${tx.getId()}")
        val cancelledTx = CancelledTx(tx)
        if (tx.getDirection() == Tx.Direction.OUTBOUND) {
            listeners[walletContextId]?.onTxCancelled(cancelledTx, rejectionReasonInt)
        }
    }

    private var oldBaseNodeStatusMessage = ""
    fun onBaseNodeStatus(contextPtr: ByteArray, baseNodeStatePointer: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val baseNodeState = FFITariBaseNodeState(baseNodeStatePointer).runWithDestroy { TariBaseNodeState(it) }

        val newMessage = "Base node state changed: $baseNodeState"
        log(walletContextId, newMessage, oldBaseNodeStatusMessage)
        oldBaseNodeStatusMessage = newMessage

        listeners[walletContextId]?.onBaseNodeStateChanged(baseNodeState)
    }

    fun onConnectivityStatus(contextPtr: ByteArray, bytes: ByteArray) {
        // FIXME: not used anymore, should be removed once FFI is updated
    }

    fun onWalletScannedHeight(contextPtr: ByteArray, bytes: ByteArray) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val height = BigInteger(1, bytes)
        log(walletContextId, "Wallet scanned height is [$height]")
        listeners[walletContextId]?.onWalletScannedHeight(height.toInt())
    }

    fun onBalanceUpdated(contextPtr: ByteArray, ptr: FFIPointer) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val balance = FFIBalance(ptr).runWithDestroy { BalanceInfo(it.getAvailable(), it.getIncoming(), it.getOutgoing(), it.getTimeLocked()) }
        log(
            walletContextId = walletContextId,
            message = "Balance Updated: " +
                    "${balance.availableBalance.formattedTariValue} available, " +
                    "${balance.pendingIncomingBalance.formattedTariValue} pending incoming, " +
                    "${balance.pendingOutgoingBalance.formattedTariValue} pending outgoing, " +
                    "${balance.timeLockedBalance.formattedTariValue} time locked",
        )
        listeners[walletContextId]?.onBalanceUpdated(balance)
    }

    fun onTXOValidationComplete(contextPtr: ByteArray, bytes: ByteArray, statusBytes: ByteArray) {
        // FIXME: not used anymore, should be removed once FFI is updated
    }

    fun onTxValidationComplete(contextPtr: ByteArray, requestIdBytes: ByteArray, statusBytes: ByteArray) {
        // FIXME: not used anymore, should be removed once FFI is updated
    }

    fun onContactLivenessDataUpdated(contextPtr: ByteArray, livenessUpdate: FFIPointer) {
        // FIXME: not used anymore, should be removed once FFI is updated
    }

    fun onWalletRecovery(contextPtr: ByteArray, event: Int, firstArg: ByteArray, secondArg: ByteArray) {
        val walletContextId = BigInteger(1, contextPtr).toInt()
        val state = WalletRestorationState.create(event, firstArg, secondArg)
        log(
            walletContextId = walletContextId,
            message = "Wallet restoration: ${
                when (state) {
                    is WalletRestorationState.NotStarted -> "Not started"
                    is WalletRestorationState.Progress -> "Progress: ${state.currentBlock}/${state.numberOfBlocks}"
                    is WalletRestorationState.Completed -> "Completed: ${state.numberOfUTXO} UTXOs, ${state.microTari.size} MicroTari"
                    is WalletRestorationState.ScanningRoundFailed -> "Scanning round failed: ${state.retryCount}/${state.retryLimit}"
                }
            }"
        )
        listeners[walletContextId]?.onWalletRestoration(state)
    }

    private fun log(walletContextId: Int, message: String, oldMessage: String = "") {
        if (message == oldMessage) return
        logger.i("${if (walletContextId == PAPER_WALLET_CONTEXT_ID) "(Paper wallet) " else ""}$message")
    }

    companion object {
        const val MAIN_WALLET_CONTEXT_ID = 1001
        const val PAPER_WALLET_CONTEXT_ID = 1002
    }
}

interface FFIWalletListener {

    fun onTxReceived(pendingInboundTx: PendingInboundTx) = Unit
    fun onTxReplyReceived(pendingOutboundTx: PendingOutboundTx) = Unit
    fun onTxFinalized(pendingInboundTx: PendingInboundTx) = Unit
    fun onInboundTxBroadcast(pendingInboundTx: PendingInboundTx) = Unit
    fun onOutboundTxBroadcast(pendingOutboundTx: PendingOutboundTx) = Unit
    fun onTxMined(completedTx: CompletedTx) = Unit
    fun onTxMinedUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = Unit
    fun onTxFauxConfirmed(completedTx: CompletedTx) = Unit
    fun onTxFauxUnconfirmed(completedTx: CompletedTx, confirmationCount: Int) = Unit
    fun onTxCancelled(cancelledTx: CancelledTx, rejectionReason: Int) = Unit
    fun onBalanceUpdated(balanceInfo: BalanceInfo) = Unit
    fun onWalletRestoration(state: WalletRestorationState) = Unit
    fun onWalletScannedHeight(height: Int) = Unit
    fun onBaseNodeStateChanged(baseNodeState: TariBaseNodeState) = Unit
}