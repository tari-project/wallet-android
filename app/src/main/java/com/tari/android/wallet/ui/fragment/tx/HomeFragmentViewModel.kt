package com.tari.android.wallet.ui.fragment.tx


import android.text.SpannableString
import android.text.Spanned
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
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager.StagedSecurityEffect
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.WalletSecurityStage
import com.tari.android.wallet.data.sharedPrefs.securityStages.modules.SecurityStageHeadModule
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.safeCastTo
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
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item.BackupOnboardingArgs
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModule
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.util.extractEmojis
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import yat.android.ui.extension.HtmlHelper
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

    @Inject
    lateinit var stagedWalletSecurityManager: StagedWalletSecurityManager

    private val _balanceInfo = MutableLiveData<BalanceInfo>()
    val balanceInfo: LiveData<BalanceInfo> = _balanceInfo

    private val _refreshBalanceInfo = SingleLiveEvent<Boolean>()
    val refreshBalanceInfo: SingleLiveEvent<Boolean> = _refreshBalanceInfo

    val txList = MediatorLiveData<List<CommonViewHolderItem>>()

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
        txList.postValue(list.filterIsInstance<TransactionItem>().sortedByDescending { it.tx.timestamp }.take(TRANSACTION_AMOUNT_HOME_PAGE))
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
        walletService.getWithError { error, service -> service.getBalanceInfo(error) }?.let { balanceInfo ->
            _balanceInfo.postValue(balanceInfo)

            stagedWalletSecurityManager.handleBalanceChange(balanceInfo)
                .safeCastTo<StagedSecurityEffect.ShowStagedSecurityPopUp>()
                ?.let { effect ->
                    when (effect.stage) {
                        WalletSecurityStage.Stage1A -> showStagePopUp1A()
                        WalletSecurityStage.Stage1B -> showStagePopUp1B()
                        WalletSecurityStage.Stage2 -> showStagePopUp2()
                        WalletSecurityStage.Stage3 -> showStagePopUp3()
                    }
                }
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

    /**
     * Show staged security popups
     */

    private fun showStagePopUp1A() {
        showPopup(
            stage = BackupOnboardingArgs.StageOne(resourceManager, this::openStage1),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_1a_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_1a_subtitle),
            body = null,
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_1a_buttons_positive),
            bodyHtml = HtmlHelper.getSpannedText(resourceManager.getString(R.string.staged_wallet_security_stages_1a_message)),
            positiveAction = { openStage1() },
        )
    }

    private fun showStagePopUp1B() {
        showPopup(
            stage = BackupOnboardingArgs.StageTwo(resourceManager, this::openStage1B),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_1b_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_1b_subtitle),
            body = resourceManager.getString(R.string.staged_wallet_security_stages_1b_message),
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_1b_buttons_positive),
            positiveAction = { openStage1() },
        )
    }

    private fun showStagePopUp2() {
        showPopup(
            stage = BackupOnboardingArgs.StageThree(resourceManager, this::openStage2),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_2_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_2_subtitle),
            body = resourceManager.getString(R.string.staged_wallet_security_stages_2_message),
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_2_buttons_positive),
            positiveAction = { openStage2() },
        )
    }

    private fun showStagePopUp3() {
        showPopup(
            stage = BackupOnboardingArgs.StageFour(resourceManager, this::openStage3),
            titleEmoji = resourceManager.getString(R.string.staged_wallet_security_stages_3_title),
            title = resourceManager.getString(R.string.staged_wallet_security_stages_3_subtitle),
            body = resourceManager.getString(R.string.staged_wallet_security_stages_3_message),
            positiveButtonTitle = resourceManager.getString(R.string.staged_wallet_security_stages_3_buttons_positive),
            positiveAction = { openStage3() },
        )
    }

    private fun openStage1() {
        dismissDialog.postValue(Unit)
        tariNavigator.let {
            it.toAllSettings()
            it.toBackupSettings(false)
            it.toWalletBackupWithRecoveryPhrase()
        }
    }

    private fun openStage1B() {
        dismissDialog.postValue(Unit)
        tariNavigator.let {
            it.toAllSettings()
            it.toBackupSettings(true)
        }
    }

    private fun openStage2() {
        dismissDialog.postValue(Unit)
        tariNavigator.let {
            it.toAllSettings()
            it.toBackupSettings(false)
            it.toChangePassword()
        }
    }

    private fun openStage3() {
        dismissDialog.postValue(Unit)
        //todo for future
    }

    private fun showPopup(
        stage: BackupOnboardingArgs,
        titleEmoji: String,
        title: String,
        body: String?,
        positiveButtonTitle: String,
        bodyHtml: Spanned? = null,
        positiveAction: () -> Unit = {},
    ) {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                SecurityStageHeadModule(titleEmoji, title) { showBackupInfo(stage) },
                BodyModule(body, bodyHtml?.let { SpannableString(it) }),
                ButtonModule(positiveButtonTitle, ButtonStyle.Normal) { positiveAction.invoke() },
                ButtonModule(resourceManager.getString(R.string.staged_wallet_security_buttons_remind_me_later), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }

    private fun showBackupInfo(stage: BackupOnboardingArgs) {
        modularDialog.postValue(
            ModularDialogArgs(
                DialogArgs(), listOf(
                    BackupOnboardingFlowItemModule(stage),
                    SpaceModule(20),
                )
            )
        )
    }

    companion object {
        private const val TRANSACTION_AMOUNT_HOME_PAGE = 2
    }
}