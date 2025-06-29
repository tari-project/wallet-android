package com.tari.android.wallet.ui.screen.home.overview


import android.os.Build
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.error_no_connection_description
import com.tari.android.wallet.R.string.error_no_connection_title
import com.tari.android.wallet.R.string.error_node_unreachable_description
import com.tari.android.wallet.R.string.error_node_unreachable_title
import com.tari.android.wallet.application.securityStage.StagedWalletSecurityManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.screen.send.obsolete.finalize.FinalizeSendTxModel.TxFailureReason
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject

const val TARI_COM = "https://tari.com/"
private const val DATA_REFRESH_INTERVAL_MILLIS = 60_000L
private const val TRANSACTION_AMOUNT_HOME_PAGE = 5

class HomeOverviewViewModel : CommonViewModel() {

    @Inject
    lateinit var transactionRepository: TxRepository

    @Inject
    lateinit var sentryPrefRepository: SentryPrefRepository

    @Inject
    lateinit var stagedWalletSecurityManager: StagedWalletSecurityManager

    @Inject
    lateinit var balanceStateHandler: BalanceStateHandler

    @Inject
    lateinit var airdropRepository: AirdropRepository

    init {
        component.inject(this)
    }

    private val stagedSecurityDelegate = StagedSecurityDelegate(
        dialogHandler = this,
        stagedWalletSecurityManager = stagedWalletSecurityManager,
        resourceManager = resourceManager,
        tariNavigator = tariNavigator,
    )

    private val _uiState = MutableStateFlow(
        HomeOverviewModel.UiState(
            ticker = networkRepository.currentNetwork.ticker,
            networkName = networkRepository.currentNetwork.network.displayName,
            ffiVersion = BuildConfig.LIB_WALLET_VERSION,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(balanceStateHandler.balanceState) { balanceInfo ->
            _uiState.update { it.copy(balance = balanceInfo) }
            stagedSecurityDelegate.handleStagedSecurity(balanceInfo)
        }

        collectFlow(transactionRepository.txs.map { it.allTxs }) { txs ->
            _uiState.update {
                it.copy(
                    txList = txs.sortedByDescending { it.tx.timestamp }
                        .take(TRANSACTION_AMOUNT_HOME_PAGE),
                )
            }
        }

        collectFlow(transactionRepository.txsInitialized) { txsInitialized ->
            _uiState.update { it.copy(txListInitialized = txsInitialized) }
        }

        collectFlow(walletManager.walletEvent) { event ->
            when (event) {
                is WalletEvent.TxSend.TxSendFailed -> onTxSendFailed(event.failureReason)
                is WalletEvent.TxSend.TxSendSuccessful -> {
                    delay(300L) // Sometimes this callback arrives before we have cleared fragment navigation stack, so we need to wait a bit
                    showTxDetail(event.txId)
                }

                else -> Unit
            }
        }

        collectFlow(connectionState) { connectionState ->
            _uiState.update { it.copy(connectionState = connectionState) }
        }

        launchOnIo {
            while (true) {
                refreshData()
                delay(DATA_REFRESH_INTERVAL_MILLIS)
            }
        }

        checkForDataConsent()

        showRecoverySuccessIfNeeded()
    }

    fun refreshData() {
        launchOnIo {
            _uiState.update { it.copy(isMiningError = false) }
            walletManager.doOnWalletRunning { wallet ->
                airdropRepository.getMiningStatus(wallet)
                    .onSuccess { mining -> _uiState.update { it.copy(isMining = mining, isMiningError = false) } }
                    .onFailure { _uiState.update { it.copy(isMiningError = it.isMining == null) } }
            }
        }

        launchOnIo {
            _uiState.update { it.copy(activeMinersCountError = false) }
            airdropRepository.getMinerStats()
                .onSuccess { activeMinersCount -> _uiState.update { it.copy(activeMinersCount = activeMinersCount, activeMinersCountError = false) } }
                .onFailure { _uiState.update { it.copy(activeMinersCountError = it.activeMinersCount == null) } }
        }
    }

    fun navigateToTxDetail(tx: Tx) {
        tariNavigator.navigate(Navigation.TxList.ToTxDetails(tx))
    }

    fun checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionManager.runWithPermission(listOf(android.Manifest.permission.POST_NOTIFICATIONS)) {
                logger.i("notification permission checked successfully")
            }
        }
    }

    fun onSyncDialogDismiss() {
        sharedPrefsRepository.needToShowRecoverySuccessDialog = false
        _uiState.update {
            it.copy(
                showWalletSyncSuccessDialog = false,
                showWalletRestoreSuccessDialog = false,
            )
        }
    }

    fun onBalanceInfoClicked() {
        _uiState.update { it.copy(showBalanceInfoDialog = true) }
    }

    fun onBalanceInfoDialogDismiss() {
        _uiState.update { it.copy(showBalanceInfoDialog = false) }
    }

    fun onStartMiningClicked() {
        openUrl(TARI_COM)
    }

    fun onSendTariClicked() {
        tariNavigator.navigate(Navigation.ContactBook.ToSelectTariUser)
    }

    fun onRequestTariClicked() {
        tariNavigator.navigate(Navigation.TxList.ToReceive)
    }

    fun onAllTxClicked() {
        tariNavigator.navigate(Navigation.TxList.HomeTransactionHistory)
    }

    private fun checkForDataConsent() {
        if (sentryPrefRepository.isEnabled == null) {
            sentryPrefRepository.isEnabled = false
            showModularDialog(
                ModularDialogArgs(
                    DialogArgs(cancelable = false, canceledOnTouchOutside = false), listOf(
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
        if (sharedPrefsRepository.needToShowRecoverySuccessDialog) {
            if (sharedPrefsRepository.airdropAnonId != null) { // anonId isn't null while wallet syncing from paper wallet seeds
                _uiState.update { it.copy(showWalletSyncSuccessDialog = true) }
            } else {
                _uiState.update { it.copy(showWalletRestoreSuccessDialog = true) }
            }
        }
    }

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

    private fun showTxDetail(txId: TxId) {
        transactionRepository.findTxById(txId)?.tx?.let { tx ->
            // show close button because it's a step in the send flow
            tariNavigator.navigate(Navigation.TxList.ToTxDetails(tx, showCloseButton = true))
        } ?: run {
            logger.e("Transaction with ID $txId not found, but it was supposed to be sent")
        }
    }

    fun toggleBalanceHidden() {
        _uiState.update { it.copy(balanceHidden = !it.balanceHidden) }
    }
}