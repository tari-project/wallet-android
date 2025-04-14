package com.tari.android.wallet.ui.screen.tx.details

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.common_are_you_sure
import com.tari.android.wallet.R.string.tx_details_cancel_dialog_cancel
import com.tari.android.wallet.R.string.tx_details_cancel_dialog_description
import com.tari.android.wallet.R.string.tx_details_cancel_dialog_not_cancel
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.contacts.model.splitAlias
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
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.model.tx.Tx.Direction.INBOUND
import com.tari.android.wallet.model.tx.Tx.Direction.OUTBOUND
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsModel.TX_EXTRA_KEY
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnMain
import com.tari.android.wallet.util.extension.string
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TxDetailsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        TxDetailsModel.UiState(
            tx = savedState.getOrThrow<Tx>(TX_EXTRA_KEY),
            requiredConfirmationCount = walletManager.requireWalletInstance.getRequiredConfirmationCount(),
            blockExplorerUrl = networkRepository.currentNetwork.blockExplorerBaseUrl,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(contactsRepository.contactList.map { contactsRepository.getContactForTx(this.uiState.value.tx) }) { contact ->
            _uiState.update { it.copy(contact = contact) }
        }

        collectFlow(walletManager.walletEvent) { event ->
            when (event) {
                is WalletEvent.Tx.InboundTxBroadcast -> updateTxData(event.tx)
                is WalletEvent.Tx.OutboundTxBroadcast -> updateTxData(event.tx)
                is WalletEvent.Tx.TxFinalized -> updateTxData(event.tx)
                is WalletEvent.Tx.TxMined -> updateTxData(event.tx)
                is WalletEvent.Tx.TxMinedUnconfirmed -> updateTxData(event.tx)
                is WalletEvent.Tx.TxReplyReceived -> updateTxData(event.tx)
                is WalletEvent.Tx.TxCancelled -> updateTxData(event.tx)
                else -> Unit
            }
        }
    }

    fun addOrEditContact() = showEditNameInputs()

    fun openInBlockExplorer() {
        _openLink.postValue(uiState.value.blockExplorerLink.orEmpty())
    }

    fun onTransactionCancel() {
        if (uiState.value.tx is PendingOutboundTx && uiState.value.tx.direction == OUTBOUND && uiState.value.tx.status == PENDING) {
            showTxCancelDialog()
        }
    }

    fun onAddressDetailsClicked() {
        val walletAddress = uiState.value.contact?.walletAddress ?: return

        showAddressDetailsDialog(walletAddress)
    }

    fun showTxFeeToolTip() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.tx_detail_fee_tooltip_transaction_fee),
            description = resourceManager.getString(R.string.tx_detail_fee_tooltip_desc),
        )
    }

    private fun setTx(tx: Tx) {
        _uiState.update { it.copy(tx = tx) }
    }

    private fun cancelTransaction() {
        val isCancelled = walletManager.requireWalletInstance.cancelPendingTx(this.uiState.value.tx.id)
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

    private fun updateTxData(tx: Tx) {
        if (tx.id == this.uiState.value.tx.id) {
            setTx(tx)
        }
    }

    private fun showEditNameInputs() {
        val contact = uiState.value.contact ?: return

        val name = (contact.contactInfo.firstName + " " + contact.contactInfo.lastName).trim()

        var saveAction: () -> Boolean = { false }

        val nameModule = InputModule(
            value = name,
            hint = resourceManager.getString(R.string.contact_book_add_contact_first_name_hint),
            isFirst = true,
            isEnd = false,
            onDoneAction = { saveAction.invoke() },
        )

        val headModule = HeadModule(
            title = resourceManager.getString(R.string.contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button),
            rightButtonAction = { saveAction.invoke() },
        )

        saveAction = {
            saveDetails(nameModule.value)
            true
        }

        showInputModalDialog(
            headModule,
            nameModule,
        )
    }

    private fun saveDetails(newName: String) {
        launchOnMain {
            val firstName = splitAlias(newName).firstName
            val lastName = splitAlias(newName).lastName
            val contactDto = uiState.value.contact!!
            _uiState.update { it.copy(contact = contactsRepository.updateContactInfo(contactDto, firstName, lastName, contactDto.yat)) }
            hideDialog()
        }
    }
}

fun Tx.statusString(context: Context, requiredConfirmationCount: Long?): String {
    val confirmationCount = if (this is CompletedTx) this.confirmationCount.toInt() else null

    return if (this is CancelledTx) "" else when (this.status) {
        PENDING -> when (this.direction) {
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
