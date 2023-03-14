package com.tari.android.wallet.ui.fragment.tx


import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R.string.error_no_connection_description
import com.tari.android.wallet.R.string.error_no_connection_title
import com.tari.android.wallet.R.string.error_node_unreachable_description
import com.tari.android.wallet.R.string.error_node_unreachable_title
import com.tari.android.wallet.R.string.ffi_validation_error_cancel
import com.tari.android.wallet.R.string.ffi_validation_error_delete
import com.tari.android.wallet.R.string.ffi_validation_error_message
import com.tari.android.wallet.R.string.ffi_validation_error_title
import com.tari.android.wallet.R.string.home_completed_transactions_title
import com.tari.android.wallet.R.string.home_pending_transactions_title
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.debounce
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.ffi.FFITxCancellationReason
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.extension.toLiveData
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.tx.ui.progressController.UpdateProgressViewController
import com.tari.android.wallet.util.Build
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

class TxListViewModel : CommonViewModel() {

    @Inject
    lateinit var repository: GIFRepository

    @Inject
    lateinit var gifRepository: GIFRepository

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val stagedWalletSecurityManager = StagedWalletSecurityManager()
    val migrationManager: MigrationManager = MigrationManager()

    lateinit var progressControllerState: UpdateProgressViewController.UpdateProgressState

    private val cancelledTxs = CopyOnWriteArrayList<CancelledTx>()
    private val completedTxs = CopyOnWriteArrayList<CompletedTx>()
    private val pendingInboundTxs = CopyOnWriteArrayList<PendingInboundTx>()
    private val pendingOutboundTxs = CopyOnWriteArrayList<PendingOutboundTx>()

    private val _navigation = SingleLiveEvent<TxListNavigation>()
    val navigation: LiveData<TxListNavigation> = _navigation

    private val _connected = SingleLiveEvent<Unit>()
    val connected: LiveData<Unit> = _connected

    private val _balanceInfo = MutableLiveData<BalanceInfo>()
    val balanceInfo: LiveData<BalanceInfo> = _balanceInfo

    private val _refreshBalanceInfo = SingleLiveEvent<Boolean>()
    val refreshBalanceInfo: SingleLiveEvent<Boolean> = _refreshBalanceInfo

    private val _requiredConfirmationCount = MutableLiveData<Long>(3)
    val requiredConfirmationCount: LiveData<Long> = _requiredConfirmationCount

    private val _list = MutableLiveData<MutableList<CommonViewHolderItem>>(mutableListOf())
    val list: LiveData<MutableList<CommonViewHolderItem>> = _list

    private val _listUpdateTrigger = MediatorLiveData<Unit>()
    val listUpdateTrigger: LiveData<Unit> = _listUpdateTrigger

    val debouncedList = Transformations.map(listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE)) {
        updateList()
        refreshBalance()
    }

    private val _txSendSuccessful = SingleLiveEvent<Unit>()
    val txSendSuccessful: MutableLiveData<Unit> = _txSendSuccessful

    init {
        component.inject(this)

        migrationManager.validateVersion({ }, { showIncompatibleVersionDialog() })

        doOnConnectedToWallet { doOnConnected { runCatching { onServiceConnected() } } }
    }

    val txListIsEmpty: Boolean
        get() = cancelledTxs.isEmpty()
                && completedTxs.isEmpty()
                && pendingInboundTxs.isEmpty()
                && pendingOutboundTxs.isEmpty()

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is TransactionItem) {
            _navigation.postValue(TxListNavigation.ToTxDetails(item.tx))
        }
    }

    private fun onServiceConnected() {
        subscribeToEventBus()

        _listUpdateTrigger.addSource(contactsRepository.publishSubject.toLiveData(BackpressureStrategy.LATEST)) { _listUpdateTrigger.postValue(Unit) }

        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            fetchBalanceInfoData()
            fetchRequiredConfirmationCount()
            updateList()
            _connected.postValue(Unit)
        }
    }

    private fun updateTxListData() {
        cancelledTxs.repopulate(walletService.getWithError { error, service -> service.getCancelledTxs(error) })
        completedTxs.repopulate(walletService.getWithError { error, service -> service.getCompletedTxs(error) })
        pendingInboundTxs.repopulate(walletService.getWithError { error, service -> service.getPendingInboundTxs(error) })
        pendingOutboundTxs.repopulate(walletService.getWithError { error, service -> service.getPendingOutboundTxs(error) })
    }

    private fun fetchBalanceInfoData() {
        val balance = walletService.getWithError { error, service -> service.getBalanceInfo(error) }
        _balanceInfo.postValue(balance)
    }

    private fun fetchRequiredConfirmationCount() {
        _requiredConfirmationCount.postValue(walletService.getWithError { error, service -> service.getRequiredConfirmationCount(error) })
    }

    fun refreshAllData(isRestarted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            refreshBalance(isRestarted)
            updateList()
        }
    }

    private fun refreshBalance(isRestarted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            fetchBalanceInfoData()
            _refreshBalanceInfo.postValue(isRestarted)
        }
    }

    private fun updateList() = viewModelScope.launch(Dispatchers.Main) {
        val confirmationCount = requiredConfirmationCount.value!!

        val items = mutableListOf<CommonViewHolderItem>()

        val minedUnconfirmedTxs = completedTxs.filter { it.status == TxStatus.MINED_UNCONFIRMED }
        val nonMinedUnconfirmedCompletedTxs = completedTxs.filter { it.status != TxStatus.MINED_UNCONFIRMED }

        // sort and add pending txs
        val pendingTxs = (pendingInboundTxs + pendingOutboundTxs + minedUnconfirmedTxs).toMutableList()
        pendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        if (pendingTxs.isNotEmpty()) {
            items.add(TitleViewHolderItem(resourceManager.getString(home_pending_transactions_title), true))
            items.addAll(pendingTxs.mapIndexed { index, tx ->
                TransactionItem(
                    tx,
                    contactsRepository.ffiBridge.getContactForTx(tx),
                    index,
                    GIFViewModel(gifRepository),
                    confirmationCount
                )
            })
        }

        // sort and add non-pending txs
        val nonPendingTxs = (cancelledTxs + nonMinedUnconfirmedCompletedTxs).toMutableList()
        nonPendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        if (nonPendingTxs.isNotEmpty()) {
            items.add(TitleViewHolderItem(resourceManager.getString(home_completed_transactions_title), false))
            items.addAll(nonPendingTxs.mapIndexed { index, tx ->
                TransactionItem(
                    tx,
                    contactsRepository.ffiBridge.getContactForTx(tx),
                    index + pendingTxs.size,
                    GIFViewModel(gifRepository),
                    confirmationCount
                )
            })
        }
        _list.postValue(items)
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Transaction.Updated>(this) { refreshAllData() }
        EventBus.subscribe<Event.Transaction.TxReceived>(this) {
            if (progressControllerState.state != UpdateProgressViewController.State.RECEIVING) {
                onTxReceived(it.tx)
            }
        }
        EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { onTxReplyReceived(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFinalized>(this) { onTxFinalized(it.tx) }
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { onInboundTxBroadcast(it.tx) }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { onOutboundTxBroadcast(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { onTxMinedUnconfirmed(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { onTxMined(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { onTxFauxMinedUnconfirmed(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { onFauxTxMined(it.tx) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) {
            if (progressControllerState.state != UpdateProgressViewController.State.RECEIVING) {
                onTxCancelled(it.tx)
            }
        }

        EventBus.subscribe<Event.Transaction.TxSendSuccessful>(this) { onTxSendSuccessful(it.txId) }
        EventBus.subscribe<Event.Transaction.TxSendFailed>(this) { onTxSendFailed(it.failureReason) }

        EventBus.balanceState.publishSubject.subscribe { _balanceInfo.postValue(it) }.addTo(compositeDisposable)
    }

    private fun onTxReceived(tx: PendingInboundTx) {
        pendingInboundTxs.add(tx)

        fetchBalanceInfoData()
        _refreshBalanceInfo.postValue(false)
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxReplyReceived(tx: PendingOutboundTx) {
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
        fetchBalanceInfoData()
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxFinalized(tx: PendingInboundTx) {
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
        fetchBalanceInfoData()
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onInboundTxBroadcast(tx: PendingInboundTx) {
        // just update data - no UI change required
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onOutboundTxBroadcast(tx: PendingOutboundTx) {
        // just update data - no UI change required
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onTxMinedUnconfirmed(tx: CompletedTx) {
        when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }.removeIf { it.id == tx.id }
        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxMined(tx: CompletedTx) {
        pendingInboundTxs.removeIf { it.id == tx.id }
        pendingOutboundTxs.removeIf { it.id == tx.id }

        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxFauxMinedUnconfirmed(tx: CompletedTx) {
        when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }.removeIf { it.id == tx.id }
        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onFauxTxMined(tx: CompletedTx) {
        pendingInboundTxs.removeIf { it.id == tx.id }
        pendingOutboundTxs.removeIf { it.id == tx.id }

        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxCancelled(tx: CancelledTx) {
        val source = when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }
        source.find { it.id == tx.id }?.let { source.remove(it) }
        cancelledTxs.add(tx)
        fetchBalanceInfoData()
        _refreshBalanceInfo.postValue(false)
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxSendSuccessful(txId: TxId) {
        _txSendSuccessful.postValue(Unit)

        viewModelScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val tx = walletService.getPendingOutboundTxById(txId, error)
            if (error == WalletError.NoError) {
                pendingOutboundTxs.add(tx)
                _listUpdateTrigger.postValue(Unit)
            } else {
                refreshAllData()
            }
        }

        refreshBalance(true)
    }

    /**
     * Called when an outgoing transaction has failed.
     */
    private fun onTxSendFailed(failureReason: TxFailureReason) = when (failureReason) {
        TxFailureReason.NETWORK_CONNECTION_ERROR -> displayNetworkConnectionErrorDialog()
        TxFailureReason.BASE_NODE_CONNECTION_ERROR, TxFailureReason.SEND_ERROR -> displayBaseNodeConnectionErrorDialog()
    }

    private fun displayNetworkConnectionErrorDialog() {
        val errorDialogArgs = ErrorDialogArgs(
            resourceManager.getString(error_no_connection_title),
            resourceManager.getString(error_no_connection_description),
        )
        _modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
    }

    private fun displayBaseNodeConnectionErrorDialog() {
        val errorDialogArgs = ErrorDialogArgs(
            resourceManager.getString(error_node_unreachable_title),
            resourceManager.getString(error_node_unreachable_description),
        )
        _modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
    }

    fun showIncompatibleVersionDialog() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(ffi_validation_error_title)),
                BodyModule(resourceManager.getString(ffi_validation_error_message)),
                ButtonModule(resourceManager.getString(ffi_validation_error_delete), ButtonStyle.Warning) {
                    _dismissDialog.postValue(Unit)
                    deleteWallet()
                },
                ButtonModule(resourceManager.getString(ffi_validation_error_cancel), ButtonStyle.Close) {
                    _dismissDialog.postValue(Unit)
                }
            ))
        _modularDialog.postValue(args)
    }

    private fun deleteWallet() {
        // disable CTAs
        viewModelScope.launch(Dispatchers.IO) {
            walletServiceLauncher.stopAndDelete()
            _navigation.postValue(TxListNavigation.ToSplashScreen)
        }
    }

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 500L
    }
}