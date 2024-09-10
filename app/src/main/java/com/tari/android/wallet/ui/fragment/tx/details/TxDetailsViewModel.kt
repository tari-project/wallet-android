package com.tari.android.wallet.ui.fragment.tx.details

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_are_you_sure
import com.tari.android.wallet.R.string.tx_details_cancel_dialog_cancel
import com.tari.android.wallet.R.string.tx_details_cancel_dialog_description
import com.tari.android.wallet.R.string.tx_details_cancel_dialog_not_cancel
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.Tx.Direction.INBOUND
import com.tari.android.wallet.model.Tx.Direction.OUTBOUND
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus.BROADCAST
import com.tari.android.wallet.model.TxStatus.COINBASE
import com.tari.android.wallet.model.TxStatus.COINBASE_CONFIRMED
import com.tari.android.wallet.model.TxStatus.COINBASE_NOT_IN_BLOCKCHAIN
import com.tari.android.wallet.model.TxStatus.COINBASE_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.COMPLETED
import com.tari.android.wallet.model.TxStatus.IMPORTED
import com.tari.android.wallet.model.TxStatus.MINED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.MINED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.PENDING
import com.tari.android.wallet.model.TxStatus.QUEUED
import com.tari.android.wallet.model.TxStatus.REJECTED
import com.tari.android.wallet.model.TxStatus.TX_NULL_ERROR
import com.tari.android.wallet.model.TxStatus.UNKNOWN
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.splitAlias
import com.tari.android.wallet.ui.fragment.tx.details.TxDetailsFragment.Companion.TX_EXTRA_KEY
import com.tari.android.wallet.ui.fragment.tx.details.TxDetailsFragment.Companion.TX_ID_EXTRA_KEY
import com.tari.android.wallet.ui.fragment.tx.details.gif.TxState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TxDetailsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val requiredConfirmationCount: Long? = FFIWallet.getOrNull { it.getRequiredConfirmationCount() }?.toLong()

    private var txId: TxId? = savedState.get<TxId>(TX_ID_EXTRA_KEY)

    private val _tx = MutableStateFlow(savedState.get<Tx>(TX_EXTRA_KEY))
    val tx = _tx.asStateFlow()
    private val txValue: Tx
        get() = _tx.value ?: error("Tx object is not initialized")

    private val _cancellationReason = MutableLiveData<String>()
    val cancellationReason: LiveData<String> = _cancellationReason

    private val _explorerLink = MutableLiveData("")
    val explorerLink: LiveData<String> = _explorerLink

    private val _contact = MutableStateFlow<ContactDto?>(null)
    val contact = _contact.asStateFlow()

    init {
        component.inject(this)

        doOnWalletServiceConnected {
            findTxAndUpdateUI()
        }

        observeTxUpdates()

        collectFlow(contactsRepository.contactList) { updateContact() }
    }

    fun addOrEditContact() = showEditNameInputs()

    fun openInBlockExplorer() {
        _openLink.postValue(_explorerLink.value.orEmpty())
    }

    fun onTransactionCancel() {
        val tx = txValue
        if (tx is PendingOutboundTx && tx.direction == OUTBOUND && tx.status == PENDING) {
            showTxCancelDialog()
        }
    }

    fun onAddressDetailsClicked() {
        val walletAddress = contact.value?.walletAddress ?: return

        showAddressDetailsDialog(walletAddress)
    }

    private fun setTxArg(tx: Tx) {
        _tx.update { tx }
        _cancellationReason.postValue(getCancellationReason(tx))
        generateExplorerLink(tx)
        updateContact()
    }

    private fun cancelTransaction() {
        val isCancelled = walletService.getWithError { error, wallet -> wallet.cancelPendingTx(TxId(this.txValue.id), error) }
        if (!isCancelled) {
            showSimpleDialog(
                title = resourceManager.getString(R.string.tx_detail_cancellation_error_title),
                description = resourceManager.getString(R.string.tx_detail_cancellation_error_description),
            )
        }
    }

    private fun showTxCancelDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(common_are_you_sure)),
            BodyModule(resourceManager.getString(tx_details_cancel_dialog_description)),
            ButtonModule(resourceManager.getString(tx_details_cancel_dialog_cancel), ButtonStyle.Normal) {
                cancelTransaction()
                hideDialog()
            },
            ButtonModule(resourceManager.getString(tx_details_cancel_dialog_not_cancel), ButtonStyle.Close),
        )
    }

    private fun updateContact() {
        this.tx.value?.let { tx ->
            val contact = contactsRepository.getContactForTx(tx)
            _contact.update { contact }
        }
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
            showSimpleDialog(
                title = resourceManager.getString(R.string.tx_details_error_tx_not_found_title),
                description = resourceManager.getString(R.string.tx_details_error_tx_not_found_desc),
                onClose = { backPressed.call() },
            )
        } else {
            setTxArg(foundTx)
        }
    }

    private fun generateExplorerLink(tx: Tx) {
        (tx as? CompletedTx)?.txKernel?.let { txKernel ->
            _explorerLink.postValue(
                resourceManager.getString(
                    R.string.explorer_kernel_url,
                    networkRepository.currentNetwork.blockExplorerUrl.orEmpty(),
                    txKernel.publicNonce,
                    txKernel.signature,
                )
            )
        }
    }

    private fun findTxById(id: TxId, walletService: TariWalletService): Tx? =
        runCatching { walletService.getPendingInboundTxById(id, WalletError()) }.getOrNull()
            ?: runCatching { walletService.getPendingOutboundTxById(id, WalletError()) }.getOrNull()
            ?: runCatching { walletService.getCompletedTxById(id, WalletError()) }.getOrNull()
            ?: runCatching { walletService.getCancelledTxById(id, WalletError()) }.getOrNull()

    private fun showEditNameInputs() {
        val contact = contact.value!!

        val name = (contact.contactInfo.firstName + " " + contact.contactInfo.lastName).trim()

        var saveAction: () -> Boolean = { false }

        val nameModule =
            InputModule(
                value = name,
                hint = resourceManager.getString(R.string.contact_book_add_contact_first_name_hint),
                isFirst = true,
                isEnd = false,
            ) { saveAction.invoke() }

        val headModule = HeadModule(
            title = resourceManager.getString(R.string.contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button),
        ) { saveAction.invoke() }

        val moduleList = mutableListOf(headModule, nameModule)

        saveAction = {
            saveDetails(nameModule.value)
            true
        }

        showInputModalDialog(ModularDialogArgs(DialogArgs(), moduleList))
    }

    private fun saveDetails(newName: String) {
        launchOnMain {
            val firstName = splitAlias(newName).firstName
            val lastName = splitAlias(newName).lastName
            val contactDto = contact.value!!
            _contact.update { contactsRepository.updateContactInfo(contactDto, firstName, lastName, contactDto.yatDto?.yat.orEmpty()) }
            hideDialog()
        }
    }
}

fun Tx.statusString(context: Context, requiredConfirmationCount: Long?): String {
    val state = TxState.from(this)
    val confirmationCount = if (this is CompletedTx) this.confirmationCount.toInt() else null

    return if (this is CancelledTx) "" else when (state.status) {
        PENDING -> when (state.direction) {
            INBOUND -> context.string(R.string.tx_detail_waiting_for_sender_to_complete)
            OUTBOUND -> context.string(R.string.tx_detail_waiting_for_recipient)
        }

        BROADCAST, COMPLETED -> if (requiredConfirmationCount != null) {
            context.string(R.string.tx_detail_completing_final_processing_with_step, 1, requiredConfirmationCount + 1)
        } else {
            context.string(R.string.tx_detail_completing_final_processing)
        }

        MINED_UNCONFIRMED -> if (confirmationCount != null && requiredConfirmationCount != null) {
            context.string(R.string.tx_detail_completing_final_processing_with_step, confirmationCount, requiredConfirmationCount + 1)
        } else {
            context.string(R.string.tx_detail_completing_final_processing)
        }

        TX_NULL_ERROR, IMPORTED, COINBASE, MINED_CONFIRMED, REJECTED, ONE_SIDED_UNCONFIRMED, ONE_SIDED_CONFIRMED, QUEUED, COINBASE_UNCONFIRMED,
        COINBASE_CONFIRMED, COINBASE_NOT_IN_BLOCKCHAIN, UNKNOWN -> ""
    }
}
