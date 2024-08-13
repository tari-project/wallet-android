package com.tari.android.wallet.ui.fragment.tx.details

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
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.Tx.Direction.OUTBOUND
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus.PENDING
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.addressDetails.AddressDetailsModule
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.splitAlias
import com.tari.android.wallet.ui.fragment.tx.details.TxDetailsFragment.Companion.TX_EXTRA_KEY
import com.tari.android.wallet.ui.fragment.tx.details.TxDetailsFragment.Companion.TX_ID_EXTRA_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TxDetailsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    var requiredConfirmationCount: Long = 0
        private set

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
            fetchRequiredConfirmationCount()
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

        showModularDialog(
            HeadModule(
                title = resourceManager.getString(R.string.wallet_info_address_details_title),
                rightButtonIcon = R.drawable.vector_common_close,
                rightButtonAction = { hideDialog() },
            ),
            AddressDetailsModule(
                tariWalletAddress = walletAddress,
                copyBase58 = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullBase58,
                    )
                },
                copyEmojis = {
                    copyToClipboard(
                        clipLabel = resourceManager.getString(R.string.wallet_info_address_copy_address_to_clipboard_label),
                        clipText = walletAddress.fullEmojiId,
                    )
                },
            )
        )
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


    private fun fetchRequiredConfirmationCount() {
        requiredConfirmationCount = walletService.getWithError { error, wallet -> wallet.getRequiredConfirmationCount(error) }
    }

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