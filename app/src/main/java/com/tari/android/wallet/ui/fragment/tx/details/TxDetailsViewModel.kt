package com.tari.android.wallet.ui.fragment.tx.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.input.InputModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import io.reactivex.BackpressureStrategy
import io.reactivex.subjects.BehaviorSubject
import javax.inject.Inject

class TxDetailsViewModel : CommonViewModel() {

    var requiredConfirmationCount: Long = 0
        private set

    private var txId: TxId? = null

    private val _txObject = BehaviorSubject.create<Tx>()

    val tx: LiveData<Tx> = _txObject.toFlowable(BackpressureStrategy.LATEST).toLiveData()

    private val _cancellationReason = MutableLiveData<String>()
    val cancellationReason: LiveData<String> = _cancellationReason

    private val _explorerLink = MutableLiveData("")
    val explorerLink: LiveData<String> = _explorerLink

    val contact = MediatorLiveData<ContactDto>()

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)

        doOnConnected {
            fetchRequiredConfirmationCount()
            findTxAndUpdateUI()
            _txObject.onNext(_txObject.value!!)
        }

        observeTxUpdates()

        contact.addSource(contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) { updateContact() }

        contact.addSource(tx) { updateContact() }
    }

    fun setTxArg(tx: Tx) {
        _txObject.onNext(tx)
        _cancellationReason.postValue(getCancellationReason(tx))
        generateExplorerLink(tx)
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
            modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
        }
    }

    fun addOrEditContact() = showEditNameInputs()

    fun openInBlockExplorer() {
        _openLink.postValue(_explorerLink.value.orEmpty())
    }

    private fun updateContact() {
        val tx = this.tx.value ?: return
        val contact = contactsRepository.ffiBridge.getContactForTx(tx)
        this.contact.postValue(contact)
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
            ) { backPressed.call() }
            modularDialog.postValue(errorArgs.getModular(resourceManager))
        } else {
            foundTx.let { _txObject.onNext(it) }
            generateExplorerLink(foundTx)
        }
    }

    private fun generateExplorerLink(tx: Tx) {
        (tx as? CompletedTx)?.txKernel?.let { txKernel ->
            _explorerLink.postValue(resourceManager.getString(R.string.explorer_kernel_url, txKernel.publicNonce, txKernel.signature))
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

    fun showEditNameInputs() {
        val contact = contact.value!!

        val name = (contact.contact.firstName + " " + contact.contact.surname).trim()

        var saveAction: () -> Boolean = { false }

        val nameModule =
            InputModule(
                value = name,
                hint = resourceManager.getString(R.string.contact_book_add_contact_first_name_hint),
                isFirst = true,
                isEnd = false,
            ) { saveAction.invoke() }

        val headModule = HeadModule(
            resourceManager.getString(R.string.contact_book_details_edit_title),
            rightButtonTitle = resourceManager.getString(R.string.contact_book_add_contact_done_button)
        ) { saveAction.invoke() }

        val moduleList = mutableListOf(headModule, nameModule)

        saveAction = {
            saveDetails(nameModule.value)
            true
        }

        val args = ModularDialogArgs(DialogArgs(), moduleList)
        _inputDialog.postValue(args)
    }

    private fun saveDetails(newName: String) {
        val split = newName.split(" ")
        val name = split.getOrNull(0).orEmpty().trim()
        val surname = split.getOrNull(1).orEmpty().trim()
        val contact = contact.value!!
        this.contact.value = contactsRepository.updateContactInfo(contact, name, surname, contact.getYatDto()?.yat.orEmpty())
        dismissDialog.postValue(Unit)
    }
}