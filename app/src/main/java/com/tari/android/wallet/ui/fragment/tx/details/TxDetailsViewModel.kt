package com.tari.android.wallet.ui.fragment.tx.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.executeWithError
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs

class TxDetailsViewModel : CommonViewModel() {

    var requiredConfirmationCount: Long = 0
        private set

    private var txId: TxId? = null

    private val _tx = MutableLiveData<Tx>()
    val tx: LiveData<Tx> = _tx

    private val _cancellationReason = MutableLiveData<String>()
    val cancellationReason: LiveData<String> = _cancellationReason

    private val _explorerLink = MutableLiveData("")
    val explorerLink: LiveData<String> = _explorerLink

    private val serviceConnection = TariWalletServiceConnection()
    private val walletService
        get() = serviceConnection.currentState.service!!

    init {
        serviceConnection.connection.subscribe {
            if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) {
                fetchRequiredConfirmationCount()
                findTxAndUpdateUI()
                _tx.value = _tx.value
            }
        }.addTo(compositeDisposable)

        observeTxUpdates()
    }

    fun setTxArg(tx: Tx) {
        _tx.postValue(tx)
        _cancellationReason.postValue(getCancellationReason(tx))
        generateExplorerLink()
    }

    fun loadTxById(txId: TxId) {
        this.txId = txId
        findTxAndUpdateUI()
    }

    fun cancelTransaction() {
        val isCancelled = walletService.getWithError { error, wallet -> wallet.cancelPendingTx(TxId(this.tx.value!!.id), error) }
        if (!isCancelled) {
            val errorDialogArgs = ErrorDialogArgs(
                resourceManager.getString(R.string.tx_detail_cancellation_error_title),
                resourceManager.getString(R.string.tx_detail_cancellation_error_description)
            )
            _modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
        }
    }

    fun removeContact() {
        val currentTx = tx.value!!
        val contact = currentTx.user as? Contact ?: return

        walletService.executeWithError { error, wallet -> wallet.removeContact(contact, error) }
        currentTx.user = User(contact.publicKey)
        EventBus.post(Event.Contact.ContactRemoved(contact.publicKey))

        _tx.value = currentTx
    }


    fun updateContactAlias(newAlias: String) {
        val tx = tx.value!!

        walletService.executeWithError { error, wallet -> wallet.updateContactAlias(tx.user.publicKey, newAlias, error) }

        tx.user = Contact(tx.user.publicKey, newAlias)
        EventBus.post(Event.Contact.ContactAddedOrUpdated(tx.user.publicKey, newAlias))
        _tx.value = tx
    }

    fun openInBlockExplorer() {
        _openLink.postValue(_explorerLink.value.orEmpty())
    }

    private fun getCancellationReason(tx: Tx): String {
        val reason = when ((tx as? CancelledTx)?.cancellationReason) {
            FFITxCancellationReason.Unknown -> R.string.tx_details_cancellation_reason_unknown
            FFITxCancellationReason.UserCancelled -> R.string.tx_details_cancellation_reason_user_cancelled
            FFITxCancellationReason.Timeout -> R.string.tx_details_cancellation_reason_timeout
            FFITxCancellationReason.DoubleSpend -> R.string.tx_details_cancellation_reason_double_spend
            FFITxCancellationReason.Orphan -> R.string.tx_details_cancellation_reason_orphan
            FFITxCancellationReason.TimeLocked -> R.string.tx_details_cancellation_reason_time_locked
            FFITxCancellationReason.InvalidTransaction -> R.string.tx_details_cancellation_reason_invalid_transaction
            FFITxCancellationReason.AbandonedCoinbase -> R.string.tx_details_cancellation_reason_abandoned_coinbase
            else -> null
        }

        return reason?.let { resourceManager.getString(it) } ?: ""
    }

    private fun observeTxUpdates() {
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFinalized>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { updateTxData(it.tx) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) { updateTxData(it.tx) }
    }

    private fun updateTxData(tx: Tx) {
        if (tx.id == this.tx.value?.id) {
            setTxArg(tx)
        }
    }

    private fun findTxAndUpdateUI() {
        txId ?: return

        val foundTx = findTxById(txId!!, walletService)

        if (foundTx == null) {
            val errorArgs = ErrorDialogArgs(
                resourceManager.getString(R.string.tx_details_error_tx_not_found_title),
                resourceManager.getString(R.string.tx_details_error_tx_not_found_desc)
            ) { _backPressed.call() }
            _modularDialog.postValue(errorArgs.getModular(resourceManager))
        } else {
            foundTx.let { _tx.value = it }
            generateExplorerLink()
        }
    }

    private fun generateExplorerLink() {
        (tx.value as? CompletedTx)?.txKernel?.let {
            val fullLink = resourceManager.getString(R.string.explorer_kernel_url) + it.publicNonce + "/" + it.signature
            _explorerLink.postValue(fullLink)
        }
    }

    private fun findTxById(id: TxId, walletService: TariWalletService): Tx? =
        runCatching { walletService.getPendingInboundTxById(id, WalletError()) }.getOrNull()
            ?: runCatching { walletService.getPendingOutboundTxById(id, WalletError()) }.getOrNull()
            ?: runCatching { walletService.getCompletedTxById(id, WalletError()) }.getOrNull()
            ?: runCatching { walletService.getCancelledTxById(id, WalletError()) }.getOrNull()


    private fun fetchRequiredConfirmationCount() {
        requiredConfirmationCount = walletService.getWithError { error, wallet -> wallet.getRequiredConfirmationCount(error) }
    }
}