package com.tari.android.wallet.ui.fragment.tx


import android.app.Activity
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.error_no_connection_description
import com.tari.android.wallet.R.string.error_no_connection_title
import com.tari.android.wallet.R.string.error_node_unreachable_description
import com.tari.android.wallet.R.string.error_node_unreachable_title
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager.StagedSecurityEffect
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.securityStages.WalletSecurityStage
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.securityStages.SecurityStageHeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.space.SpaceModule
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.finalize.TxFailureReason
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.item.BackupOnboardingArgs
import com.tari.android.wallet.ui.fragment.settings.backup.backupOnboarding.module.BackupOnboardingFlowItemModule
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.util.EmojiId
import com.tari.android.wallet.util.extractEmojis
import com.tari.android.wallet.util.shortString
import yat.android.ui.extension.HtmlHelper
import javax.inject.Inject


class HomeFragmentViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var sentryPrefRepository: SentryPrefRepository

    @Inject
    lateinit var stagedWalletSecurityManager: StagedWalletSecurityManager

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    @Inject
    lateinit var balanceStateHandler: BalanceStateHandler

    private val _balanceInfo = MutableLiveData<BalanceInfo>()
    val balanceInfo: LiveData<BalanceInfo> = _balanceInfo

    private val _refreshBalanceInfo = SingleLiveEvent<Boolean>()
    val refreshBalanceInfo: SingleLiveEvent<Boolean> = _refreshBalanceInfo

    val txList = MediatorLiveData<List<CommonViewHolderItem>>()

    val avatarEmoji = MutableLiveData<EmojiId>()

    val emojiMedium = MutableLiveData<EmojiId>()

    init {
        component.inject(this)

        txList.addSource(transactionRepository.list) { updateList() }

        collectFlow(contactsRepository.contactList) { updateList() }

        doOnWalletRunning { doOnWalletServiceConnected { runCatching { onServiceConnected() } } }

        val address = corePrefRepository.walletAddress
        emojiMedium.postValue(address.shortString())
        avatarEmoji.postValue(address.coreKeyEmojis.extractEmojis().take(1).joinToString(""))

        checkForDataConsent()

        showRecoverySuccessIfNeeded()
    }

    fun processItemClick(item: CommonViewHolderItem) {
        if (item is TransactionItem) {
            navigation.postValue(Navigation.TxListNavigation.ToTxDetails(item.tx))
        }
    }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionManager.runWithPermission(listOf(android.Manifest.permission.POST_NOTIFICATIONS)) {
                logger.i("notification permission checked successfully")
            }
        }

        grantContactsPermission()
    }

    fun grantContactsPermission() {
        permissionManager.runWithPermission(
            permissions = listOf(
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.WRITE_CONTACTS,
            ),
            silently = true,
        ) {
            launchOnIo {
                contactsRepository.grantContactPermissionAndRefresh()
            }
        }
    }

    fun handleDeeplink(context: Activity, deepLink: DeepLink) {
        deeplinkManager.execute(context, deepLink)
    }

    private fun updateList() {
        val list = transactionRepository.list.value ?: return
        txList.postValue(list.filterIsInstance<TransactionItem>().sortedByDescending { it.tx.timestamp }.take(TRANSACTION_AMOUNT_HOME_PAGE))
    }

    private fun checkForDataConsent() {
        if (sentryPrefRepository.isEnabled == null) {
            sentryPrefRepository.isEnabled = false
            showModularDialog(
                ModularDialogArgs(DialogArgs(cancelable = false, canceledOnTouchOutside = false), listOf(
                    HeadModule(resourceManager.getString(R.string.data_collection_dialog_title)),
                    BodyModule(resourceManager.getString(R.string.data_collection_dialog_description)),
                    ButtonModule(resourceManager.getString(R.string.data_collection_dialog_positive), ButtonStyle.Normal) {
                        sentryPrefRepository.isEnabled = true
                        hideDialog()
                    },
                    ButtonModule(resourceManager.getString(R.string.data_collection_dialog_negative), ButtonStyle.Close) {
                        sentryPrefRepository.isEnabled = false
                        hideDialog()
                    }
                ))
            )
        }
    }

    private fun showRecoverySuccessIfNeeded() {
        if (corePrefRepository.needToShowRecoverySuccessDialog) {
            showSimpleDialog(
                titleRes = R.string.recovery_success_dialog_title,
                descriptionRes = R.string.recovery_success_dialog_description,
                closeButtonTextRes = R.string.recovery_success_dialog_close,
                onClose = { corePrefRepository.needToShowRecoverySuccessDialog = false },
            )
        }
    }

    private fun onServiceConnected() {
        collectFlow(walletManager.walletEvent) { event ->
            when (event) {
                is WalletEvent.Tx.TxReceived,
                is WalletEvent.Tx.TxReplyReceived,
                is WalletEvent.Tx.TxFinalized,
                is WalletEvent.Tx.InboundTxBroadcast,
                is WalletEvent.Tx.OutboundTxBroadcast,
                is WalletEvent.Tx.TxMinedUnconfirmed,
                is WalletEvent.Tx.TxMined,
                is WalletEvent.Tx.TxFauxMinedUnconfirmed,
                is WalletEvent.Tx.TxFauxConfirmed,
                is WalletEvent.Tx.TxCancelled -> refreshBalance(false)

                is WalletEvent.Updated -> refreshAllData()

                is WalletEvent.TxSend.TxSendSuccessful -> refreshBalance(false)
                is WalletEvent.TxSend.TxSendFailed -> onTxSendFailed(event.failureReason)

                else -> Unit
            }
        }

        collectFlow(balanceStateHandler.balanceState) { _balanceInfo.postValue(it) }

        refreshAllData(true)
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

    private fun refreshAllData(isRestarted: Boolean = false) {
        launchOnIo {
            refreshBalance(isRestarted)
            transactionRepository.refreshAllData()
        }
    }

    private fun refreshBalance(isRestarted: Boolean = false) {
        launchOnIo {
            fetchBalanceInfoData()
            _refreshBalanceInfo.postValue(isRestarted)
        }
    }

    /**
     * Called when an outgoing transaction has failed.
     */
    private fun onTxSendFailed(failureReason: TxFailureReason) = when (failureReason) {
        TxFailureReason.NETWORK_CONNECTION_ERROR -> displayNetworkConnectionErrorDialog()
        TxFailureReason.BASE_NODE_CONNECTION_ERROR, TxFailureReason.SEND_ERROR -> displayBaseNodeConnectionErrorDialog()
    }

    private fun displayNetworkConnectionErrorDialog() {
        showSimpleDialog(
            title = resourceManager.getString(error_no_connection_title),
            description = resourceManager.getString(error_no_connection_description),
        )
    }

    private fun displayBaseNodeConnectionErrorDialog() {
        showSimpleDialog(
            title = resourceManager.getString(error_node_unreachable_title),
            description = resourceManager.getString(error_node_unreachable_description),
        )
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
        hideDialog()
        tariNavigator.let {
            it.toAllSettings()
            it.toBackupSettings(false)
            it.toWalletBackupWithRecoveryPhrase()
        }
    }

    private fun openStage1B() {
        hideDialog()
        tariNavigator.let {
            it.toAllSettings()
            it.toBackupSettings(true)
        }
    }

    private fun openStage2() {
        hideDialog()
        tariNavigator.let {
            it.toAllSettings()
            it.toBackupSettings(false)
            it.toChangePassword()
        }
    }

    private fun openStage3() {
        hideDialog()
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
        showModularDialog(
            SecurityStageHeadModule(titleEmoji, title) { showBackupInfo(stage) },
            BodyModule(body, bodyHtml?.let { SpannableString(it) }),
            ButtonModule(positiveButtonTitle, ButtonStyle.Normal) { positiveAction.invoke() },
            ButtonModule(resourceManager.getString(R.string.staged_wallet_security_buttons_remind_me_later), ButtonStyle.Close),
        )
    }

    private fun showBackupInfo(stage: BackupOnboardingArgs) {
        showModularDialog(
            BackupOnboardingFlowItemModule(stage),
            SpaceModule(20),
        )
    }

    companion object {
        private const val TRANSACTION_AMOUNT_HOME_PAGE = 2
    }
}