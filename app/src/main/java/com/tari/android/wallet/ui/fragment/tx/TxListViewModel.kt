package com.tari.android.wallet.ui.fragment.tx

import androidx.lifecycle.*
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
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
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.tx.ui.progressController.UpdateProgressViewController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.math.BigDecimal
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
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

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

        doOnConnected { onServiceConnected() }
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

            if (progressControllerState.numberOfReceivedTxs > 0) {
                showWalletBackupPromptIfNecessary()
            }
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
            items.addAll(pendingTxs.mapIndexed { index, tx -> TransactionItem(tx, index, GIFViewModel(gifRepository), confirmationCount) })
        }

        // sort and add non-pending txs
        val nonPendingTxs = (cancelledTxs + nonMinedUnconfirmedCompletedTxs).toMutableList()
        nonPendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        if (nonPendingTxs.isNotEmpty()) {
            items.add(TitleViewHolderItem(resourceManager.getString(R.string.home_completed_transactions_title), false))
            items.addAll(nonPendingTxs.mapIndexed { index, tx ->
                TransactionItem(tx, index + pendingTxs.size, GIFViewModel(gifRepository), confirmationCount)
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

        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) { onContactAddedOrUpdated(it.contactAddress, it.contactAlias) }
        EventBus.subscribe<Event.Contact.ContactRemoved>(this) { onContactRemoved(it.contactAddress) }
    }

    private fun onTxReceived(tx: PendingInboundTx) {
        pendingInboundTxs.add(tx)

        fetchBalanceInfoData()
        _refreshBalanceInfo.postValue(false)
        showWalletBackupPromptIfNecessary()
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

    private fun onContactAddedOrUpdated(tariWalletAddress: TariWalletAddress, alias: String) {
        val contact = Contact(tariWalletAddress, alias)
        (cancelledTxs.asSequence() + pendingInboundTxs + pendingOutboundTxs + completedTxs)
            .filter { it.user.walletAddress == tariWalletAddress }
            .forEach { it.user = contact }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onContactRemoved(tariWalletAddress: TariWalletAddress) {
        val user = User(tariWalletAddress)
        (cancelledTxs.asSequence() + pendingInboundTxs + pendingOutboundTxs + completedTxs)
            .filter { it.user.walletAddress == tariWalletAddress }
            .forEach { it.user = user }
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

    fun displayNetworkConnectionErrorDialog() {
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

    private fun showWalletBackupPromptIfNecessary() {
        if (!backupSettingsRepository.isShowHintDialog()) return

        val isAnyBackupEnabled = backupSettingsRepository.getOptionList.any { it.isEnable }
        if (!isAnyBackupEnabled || backupSettingsRepository.backupPassword == null) {
            backupSettingsRepository.lastBackupDialogShown = DateTime.now()
            val inboundTransactionsCount = pendingInboundTxs.size + completedTxs.asSequence().filter { it.direction == Tx.Direction.INBOUND }.count()
            val tarisAmount = balanceInfo.value!!.availableBalance.tariValue + balanceInfo.value!!.pendingIncomingBalance.tariValue
            when {
                inboundTransactionsCount >= 5
                        && tarisAmount >= BigDecimal("25000")
                        && isAnyBackupEnabled
                        && backupSettingsRepository.backupPassword == null -> showSecureYourBackupsDialog()
                inboundTransactionsCount >= 4
                        && tarisAmount >= BigDecimal("8000")
                        && !isAnyBackupEnabled -> showRepeatedBackUpPrompt()
                // Non-faucet transactions only here. Calculation is performed here to avoid
                // unnecessary calculations as previous two cases have much greater chance to happen
                pendingInboundTxs.size + completedTxs
                    .filter { it.direction == Tx.Direction.INBOUND }
                    .filterNot { it.status == TxStatus.IMPORTED }
                    .count() >= 1
                        && !isAnyBackupEnabled -> showInitialBackupPrompt()
            }
        }
    }

    private fun showInitialBackupPrompt() {
        val args = BackupWalletArgs(
            resourceManager.getString(home_back_up_wallet_initial_title_regular_part),
            resourceManager.getString(home_back_up_wallet_initial_title_highlighted_part),
            resourceManager.getString(home_back_up_wallet_initial_description),
        ) {
            _navigation.postValue(TxListNavigation.ToTTLStore)
        }
        _modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun showRepeatedBackUpPrompt() {
        val args = BackupWalletArgs(
            resourceManager.getString(home_back_up_wallet_repeated_title_regular_part),
            resourceManager.getString(home_back_up_wallet_repeated_title_highlighted_part),
            resourceManager.getString(home_back_up_wallet_repeated_description),
        ) {
            _navigation.postValue(TxListNavigation.ToTTLStore)
        }
        _modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun showSecureYourBackupsDialog() {
        val args = BackupWalletArgs(
            resourceManager.getString(home_back_up_wallet_encrypt_title),
            "",
            resourceManager.getString(home_back_up_wallet_encrypt_description),
            home_back_up_wallet_encrypt_cta,
            home_back_up_wallet_delay_encrypt_cta,
        ) {
            _navigation.postValue(TxListNavigation.ToTTLStore)
        }
        _modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun sendTariToUser(tariWalletAddress: TariWalletAddress) {
        val error = WalletError()
        val contacts = walletService.getContacts(error)
        val recipientUser = when (error) {
            WalletError.NoError -> contacts.firstOrNull { it.walletAddress == tariWalletAddress } ?: User(tariWalletAddress)
            else -> User(tariWalletAddress)
        }

        _navigation.postValue(TxListNavigation.ToSendTariToUser(recipientUser))
    }

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 500L
        private const val SECOND_UTXO_STORE_OPEN_DELAY = 3000L
    }
}