package com.tari.android.wallet.ui.fragment.tx

import androidx.lifecycle.*
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.data.sharedPrefs.testnetFaucet.TestnetFaucetRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.*
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.model.*
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
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
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadBoldSpannableModule
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.imageModule.ImageModule
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.tx.ui.UpdateProgressViewController
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.math.BigDecimal
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject

internal class TxListViewModel() : CommonViewModel() {

    @Inject
    lateinit var repository: GIFRepository

    @Inject
    lateinit var gifRepository: GIFRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var testnetRepository: TestnetFaucetRepository

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var testnetFaucetRepository: TestnetFaucetRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    lateinit var serviceConnection: TariWalletServiceConnection
    val walletService: TariWalletService
        get() = serviceConnection.currentState.service!!

    lateinit var progressControllerState: UpdateProgressViewController.UpdateProgressState

    var testnetTariRequestIsInProgress = false

    // TODO(nyarian): remove
    private var testnetTariRequestIsWaitingOnConnection = false

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

    val debouncedList = Transformations.map(listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE)) { updateList() }

    private val _txSendSuccessful = SingleLiveEvent<Unit>()
    val txSendSuccessful: MutableLiveData<Unit> = _txSendSuccessful

    init {
        component.inject(this)

        bindToWalletService()
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

    fun requestTestnetTari() {
        if (testnetTariRequestIsInProgress) return
        if (EventBus.networkConnectionState.publishSubject.value != NetworkConnectionState.CONNECTED) {
            testnetTariRequestIsWaitingOnConnection = true
        } else {
            testnetTariRequestIsWaitingOnConnection = false
            testnetTariRequestIsInProgress = true
            viewModelScope.launch(Dispatchers.IO) {
                walletService.executeWithError { error, wallet -> wallet.requestTestnetTari(error) }
            }
        }
    }

    private fun bindToWalletService() {
        serviceConnection = TariWalletServiceConnection()
        serviceConnection.connection.subscribe {
            if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) onServiceConnected()
        }.addTo(compositeDisposable)
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

    private fun refreshBalance(isRestarted: Boolean) {
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
            items.add(TitleViewHolderItem(resourceManager.getString(home_completed_transactions_title), false))
            items.addAll(nonPendingTxs.mapIndexed { index, tx ->
                TransactionItem(tx, index + pendingTxs.size, GIFViewModel(gifRepository), confirmationCount)
            })
        }
        _list.postValue(items)
    }

    private fun subscribeToEventBus() {
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
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { onTxMinedUnconfirmed(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { onTxMined(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { onTxFauxMinedUnconfirmed(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { onFauxTxMined(it.tx) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) {
            if (progressControllerState.state != UpdateProgressViewController.State.RECEIVING) {
                onTxCancelled(it.tx)
            }
        }

        EventBus.subscribe<Event.Testnet.TestnetTariRequestSuccessful>(this) { testnetTariRequestSuccessful() }
        EventBus.subscribe<Event.Testnet.TestnetTariRequestError>(this) { testnetTariRequestError() }

        EventBus.subscribe<Event.Transaction.TxSendSuccessful>(this) { onTxSendSuccessful(it.txId) }
        EventBus.subscribe<Event.Transaction.TxSendFailed>(this) { onTxSendFailed(it.failureReason) }

        EventBus.subscribe<Event.Contact.ContactAddedOrUpdated>(this) { onContactAddedOrUpdated(it.contactPublicKey, it.contactAlias) }
        EventBus.subscribe<Event.Contact.ContactRemoved>(this) { onContactRemoved(it.contactPublicKey) }

        EventBus.networkConnectionState.subscribe(this) { networkConnectionState ->
            if (testnetTariRequestIsWaitingOnConnection && networkConnectionState == NetworkConnectionState.CONNECTED
            ) {
                requestTestnetTari()
            }
        }
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

    private fun onContactAddedOrUpdated(publicKey: PublicKey, alias: String) {
        val contact = Contact(publicKey, alias)
        (cancelledTxs.asSequence() + pendingInboundTxs + pendingOutboundTxs + completedTxs)
            .filter { it.user.publicKey == publicKey }
            .forEach { it.user = contact }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onContactRemoved(publicKey: PublicKey) {
        val user = User(publicKey)
        (cancelledTxs.asSequence() + pendingInboundTxs + pendingOutboundTxs + completedTxs)
            .filter { it.user.publicKey == publicKey }
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

        // import second testnet UTXO if it hasn't been imported yet
        if (testnetRepository.testnetTariUTXOKeyList.orEmpty().isNotEmpty()) {
            importSecondUTXO()
        }
    }

    /**
     * Called when an outgoing transaction has failed.
     */
    private fun onTxSendFailed(failureReason: TxFailureReason) = when (failureReason) {
        TxFailureReason.NETWORK_CONNECTION_ERROR -> displayNetworkConnectionErrorDialog()
        TxFailureReason.BASE_NODE_CONNECTION_ERROR, TxFailureReason.SEND_ERROR -> displayBaseNodeConnectionErrorDialog()
    }

    private fun testnetTariRequestSuccessful() {
        viewModelScope.launch(Dispatchers.IO) {
            val importedTx = walletService.getWithError { error, wallet ->
                wallet.importTestnetUTXO(resourceManager.getString(first_testnet_utxo_tx_message), error)
            }
            importedTx ?: return@launch

            testnetRepository.faucetTestnetTariRequestCompleted = true
            testnetRepository.firstTestnetUTXOTxId = importedTx.id
            completedTxs.add(importedTx)
            refreshBalance(false)
            updateList()

            viewModelScope.launch(Dispatchers.IO) {
                delay(Constants.UI.Home.showTariBotDialogDelayMs)
                showTestnetTariReceivedDialog(importedTx.user.publicKey)
            }

            testnetTariRequestIsInProgress = false
        }
    }

    private fun testnetTariRequestError() {
        testnetTariRequestIsInProgress = false
        if (!networkRepository.currentNetwork?.faucetUrl.isNullOrEmpty()) {
            val description = resourceManager.getString(faucet_error_common)
            val errorDialogArgs = ErrorDialogArgs(resourceManager.getString(faucet_error_title), description)
            _modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
        }
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

        if (!backupSettingsRepository.backupIsEnabled || backupSettingsRepository.backupPassword == null) {
            backupSettingsRepository.lastBackupDialogShown = DateTime.now()
            val inboundTransactionsCount = pendingInboundTxs.size + completedTxs.asSequence().filter { it.direction == Tx.Direction.INBOUND }.count()
            val tarisAmount = balanceInfo.value!!.availableBalance.tariValue + balanceInfo.value!!.pendingIncomingBalance.tariValue
            when {
                inboundTransactionsCount >= 5
                        && tarisAmount >= BigDecimal("25000")
                        && backupSettingsRepository.backupIsEnabled
                        && backupSettingsRepository.backupPassword == null -> showSecureYourBackupsDialog()
                inboundTransactionsCount >= 4
                        && tarisAmount >= BigDecimal("8000")
                        && !backupSettingsRepository.backupIsEnabled -> showRepeatedBackUpPrompt()
                // Non-faucet transactions only here. Calculation is performed here to avoid
                // unnecessary calculations as previous two cases have much greater chance to happen
                pendingInboundTxs.size + completedTxs
                    .filter { it.direction == Tx.Direction.INBOUND }
                    .filterNot { it.status == TxStatus.IMPORTED }
                    .count() >= 1
                        && !backupSettingsRepository.backupIsEnabled -> showInitialBackupPrompt()
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

    private fun showTestnetTariReceivedDialog(testnetSenderPublicKey: PublicKey) {
        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = false), listOf(
                HeadModule(resourceManager.getString(home_tari_bot_you_got_tari_dlg_title)),
                BodyModule(resourceManager.getString(home_tari_bot_dialog_desc)),
                ButtonModule(resourceManager.getString(home_tari_bot_try_later), ButtonStyle.Normal) {
                    sendTariToUser(testnetSenderPublicKey)
                },
                ButtonModule(resourceManager.getString(home_tari_bot_try_later), ButtonStyle.Close)
            )
        )
        _modularDialog.postValue(args)
    }

    private fun showTTLStoreDialog() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                ImageModule(R.drawable.store_modal),
                HeadBoldSpannableModule(home_ttl_store_dlg_title, home_ttl_store_dlg_title_bold_part),
                BodyModule(resourceManager.getString(home_ttl_store_dlg_desciption)),
                ButtonModule(resourceManager.getString(home_ttl_store_positive_btn), ButtonStyle.Normal) {
                    _dissmissDialog.value = Unit
                    _navigation.postValue(TxListNavigation.ToTTLStore)
                },
                ButtonModule(resourceManager.getString(home_ttl_store_negative_btn), ButtonStyle.Close)
            )
        )

        _modularDialog.postValue(args)
    }

    private fun sendTariToUser(recipientPublicKey: PublicKey) {
        val error = WalletError()
        val contacts = walletService.getContacts(error)
        val recipientUser = when (error) {
            WalletError.NoError -> contacts.firstOrNull { it.publicKey == recipientPublicKey } ?: User(recipientPublicKey)
            else -> User(recipientPublicKey)
        }

        _navigation.postValue(TxListNavigation.ToSendTariToUser(recipientUser))
    }

    private fun importSecondUTXO() {
        viewModelScope.launch(Dispatchers.IO) {
            val importedTx = walletService.getWithError { error, wallet ->
                wallet.importTestnetUTXO(resourceManager.getString(second_testnet_utxo_tx_message), error)
            }
            importedTx ?: return@launch
            testnetRepository.secondTestnetUTXOTxId = importedTx.id
            completedTxs.add(importedTx)
            refreshBalance(false)
            updateList()
            delay(SECOND_UTXO_STORE_OPEN_DELAY)
            showTTLStoreDialog()
        }
    }

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 500L
        private const val SECOND_UTXO_STORE_OPEN_DELAY = 3000L
    }
}