package com.tari.android.wallet.ui.screen.send.obsolete.finalize

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_1_desc_line_1
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_1_desc_line_2
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_3_desc_line_1
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_3_desc_line_2
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.screen.send.obsolete.finalize.FinalizeSendTxModel.TxFailureReason
import com.tari.android.wallet.util.BroadcastEffectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.joda.time.DateTime
import org.joda.time.Seconds
import javax.inject.Inject

class FinalizeSendTxViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    private val transactionData: TransactionData = savedState.get<TransactionData>(KEY_TRANSACTION_DATA)!!

    private val _uiState = MutableStateFlow(
        FinalizeSendTxModel.UiState(
            steps = listOfNotNull(
                ConnectionCheckStep(),
                DiscoveryStep(),
            ),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = BroadcastEffectFlow<FinalizeSendTxModel.Effect>()
    val effect = _effect.flow

    init {
        component.inject(this)

        checkStepStatus()
    }

    fun checkStepStatus() {
        uiState.value.steps.firstOrNull { !it.isCompleted || !it.isStarted }?.let { step ->
            val isStarted = step.isStarted
            step.check()

            if (!isStarted) {
                logger.i("Step started: ${step.javaClass.simpleName}")
                launchOnMain { _effect.send(FinalizeSendTxModel.Effect.ShowNextStep(step)) }
                return
            }
        }

        if (uiState.value.isSuccess) {
            logger.i("Transaction success")
            launchOnMain { _effect.send(FinalizeSendTxModel.Effect.SendTxSuccess) }
        }
    }

    fun onYatSendTxStop() {
        trySendTxSuccess()
        trySendTxFailure()
    }

    fun trySendTxSuccess() {
        uiState.value.sentTxId?.let { txId ->
            walletManager.sendWalletEvent(WalletManager.WalletEvent.TxSend.TxSendSuccessful(txId))
            navigateBackToHome()
        }
    }


    fun trySendTxFailure() {
        uiState.value.txFailureReason?.let { txFailureReason ->
            walletManager.sendWalletEvent(WalletManager.WalletEvent.TxSend.TxSendFailed(txFailureReason))
            navigateBackToHome()
        }
    }

    fun showCancelDialog() {
        if (!uiState.value.isSuccess) {
            showModularDialog(
                dialogId = ModularDialogArgs.DialogId.DEEPLINK_PAPER_CANCEL_SEND_TX,
                HeadModule(resourceManager.getString(R.string.finalize_send_tx_sending_cancel_dialog_title)),
                BodyModule(resourceManager.getString(R.string.finalize_send_tx_sending_cancel_dialog_description)),
                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                    navigateBackToHome()
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
            )
        }
    }

    private fun navigateBackToHome() {
        hideDialogImmediately(ModularDialogArgs.DialogId.DEEPLINK_PAPER_CANCEL_SEND_TX)
        tariNavigator.navigate(Navigation.BackToHome)
    }

    private fun setFailureReason(reason: TxFailureReason) {
        _uiState.update { it.copy(txFailureReason = reason) }
        launchOnMain { _effect.send(FinalizeSendTxModel.Effect.ShowError(reason)) }
        logger.i("Transaction failed: ${reason.name}")
    }

    sealed class FinalizingStep(
        @param:StringRes val descLine1Res: Int,
        @param:StringRes val descLine2Res: Int,
    ) {
        var isStarted: Boolean = false
        var isCompleted: Boolean = false

        abstract fun execute()

        open fun check() {
            if (!isStarted) {
                isStarted = true
                execute()
            }
        }
    }

    inner class ConnectionCheckStep : FinalizingStep(
        descLine1Res = finalize_send_tx_sending_step_1_desc_line_1,
        descLine2Res = finalize_send_tx_sending_step_1_desc_line_2,
    ) {
        private var connectionCheckStartTime: DateTime? = null

        override fun check() {
            super.check()

            connectionCheckStartTime ?: return

            val secondsElapsed = Seconds.secondsBetween(connectionCheckStartTime, DateTime.now()).seconds
            if (secondsElapsed >= CONNECTION_TIMEOUT_SEC) {
                setFailureReason(TxFailureReason.NETWORK_CONNECTION_ERROR)
                isCompleted = true
                return
            }
        }

        override fun execute() = doOnWalletRunning { onServiceConnected() }

        private fun onServiceConnected() {
            // start checking network connection
            connectionCheckStartTime = DateTime.now()
            checkConnectionStatus()
        }

        private fun checkConnectionStatus() {
            // check internet connection
            if (!networkConnection.isNetworkConnected()) {
                // either not connected or Tor proxy is not running
                setFailureReason(TxFailureReason.NETWORK_CONNECTION_ERROR)
                isCompleted = true
                return
            }

            isCompleted = true
        }
    }

    inner class DiscoveryStep : FinalizingStep(
        descLine1Res = finalize_send_tx_sending_step_3_desc_line_1,
        descLine2Res = finalize_send_tx_sending_step_3_desc_line_2,
    ) {
        override fun execute() {
            launchOnIo {
                runCatching {
                    val txId = walletManager.sendTari(
                        tariContact = TariContact(transactionData.recipientContact.walletAddress),
                        amount = transactionData.amount,
                        feePerGram = transactionData.feePerGram,
                        message = transactionData.message,
                    )

                    logger.i("Tx sent: $txId")
                    _uiState.update { it.copy(sentTxId = txId) }
                }.onFailure {
                    setFailureReason(TxFailureReason.SEND_ERROR)
                }
                isCompleted = true
            }
        }
    }

    companion object {
        const val KEY_TRANSACTION_DATA = "Finalize_send_tx_fragment_key"
        const val CONNECTION_TIMEOUT_SEC = 30
    }
}