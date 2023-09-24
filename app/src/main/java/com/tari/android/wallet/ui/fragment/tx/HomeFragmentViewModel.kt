package com.tari.android.wallet.ui.fragment.tx


import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.error_no_connection_description
import com.tari.android.wallet.R.string.error_no_connection_title
import com.tari.android.wallet.R.string.error_node_unreachable_description
import com.tari.android.wallet.R.string.error_node_unreachable_title
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.util.extractEmojis
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject


class HomeFragmentViewModel : CommonViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var sentryPrefRepository: SentryPrefRepository

    val stagedWalletSecurityManager = StagedWalletSecurityManager()

    private val _balanceInfo = MutableLiveData<BalanceInfo>()
    val balanceInfo: LiveData<BalanceInfo> = _balanceInfo

    private val _refreshBalanceInfo = SingleLiveEvent<Boolean>()
    val refreshBalanceInfo: SingleLiveEvent<Boolean> = _refreshBalanceInfo

    val txList = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    val emoji = MutableLiveData<String>()

    val emojiMedium = MutableLiveData<String>()

    init {
        component.inject(this)

        txList.addSource(transactionRepository.list) { updateList() }

        txList.addSource(contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()) { updateList() }

        doOnConnectedToWallet { doOnConnected { runCatching { onServiceConnected() } } }

        val emojies = sharedPrefsWrapper.emojiId.orEmpty().extractEmojis()
        emojiMedium.postValue(emojies.take(3).joinToString(""))
        emoji.postValue(emojies.take(1).joinToString(""))

        checkForDataConsent()
    }


    private fun updateList() {
        val list = transactionRepository.list.value ?: return
        txList.postValue(list.filterIsInstance<TransactionItem>().sortedBy { it.tx.timestamp }.takeLast(amountOfTransactions).toMutableList())
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is TransactionItem) {
            navigation.postValue(Navigation.TxListNavigation.ToTxDetails(item.tx))
        }
    }

    private fun checkForDataConsent() {
        if (sentryPrefRepository.isEnabled == null) {
            sentryPrefRepository.isEnabled = false
            val args = ModularDialogArgs(DialogArgs(cancelable = false, canceledOnTouchOutside = false), listOf(
                HeadModule(resourceManager.getString(R.string.data_collection_dialog_title)),
                BodyModule(resourceManager.getString(R.string.data_collection_dialog_description)),
                ButtonModule(resourceManager.getString(R.string.data_collection_dialog_positive), ButtonStyle.Normal) {
                    sentryPrefRepository.isEnabled = true
                    dismissDialog.postValue(Unit)
                },
                ButtonModule(resourceManager.getString(R.string.data_collection_dialog_negative), ButtonStyle.Close) {
                    sentryPrefRepository.isEnabled = false
                    dismissDialog.postValue(Unit)
                }
            ))
            modularDialog.postValue(args)
        }
    }

    private fun onServiceConnected() {
        subscribeToEventBus()

        viewModelScope.launch(Dispatchers.IO) {
            refreshAllData(true)
        }
    }

    private fun fetchBalanceInfoData() {
        walletService.getWithError { error, service -> service.getBalanceInfo(error) }?.let {
            EventBus.balanceUpdates.post(it)
            _balanceInfo.postValue(it)
        }
    }

    fun refreshAllData(isRestarted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            refreshBalance(isRestarted)
            transactionRepository.refreshAllData()
        }
    }

    private fun refreshBalance(isRestarted: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            fetchBalanceInfoData()
            _refreshBalanceInfo.postValue(isRestarted)
        }
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Transaction.Updated>(this) { refreshAllData() }
        EventBus.subscribe<Event.Transaction.TxReceived>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxFinalized>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) { refreshBalance(false) }

        EventBus.subscribe<Event.Transaction.TxSendSuccessful>(this) { refreshBalance(false) }
        EventBus.subscribe<Event.Transaction.TxSendFailed>(this) { onTxSendFailed(it.failureReason) }

        EventBus.balanceState.publishSubject.subscribe { _balanceInfo.postValue(it) }.addTo(compositeDisposable)
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
        modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
    }

    private fun displayBaseNodeConnectionErrorDialog() {
        val errorDialogArgs = ErrorDialogArgs(
            resourceManager.getString(error_node_unreachable_title),
            resourceManager.getString(error_node_unreachable_description),
        )
        modularDialog.postValue(errorDialogArgs.getModular(resourceManager))
    }

    companion object {
        val amountOfTransactions = 2
    }
}