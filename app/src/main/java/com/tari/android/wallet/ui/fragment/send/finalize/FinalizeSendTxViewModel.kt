package com.tari.android.wallet.ui.fragment.send.finalize

import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_1_desc_line_1
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_1_desc_line_2
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_2_desc_line_1
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_2_desc_line_2
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_3_desc_line_1
import com.tari.android.wallet.R.string.finalize_send_tx_sending_step_3_desc_line_2
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TransactionSendStatus
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import org.joda.time.DateTime
import org.joda.time.Seconds
import javax.inject.Inject

class FinalizeSendTxViewModel : CommonViewModel() {

    @Inject
    lateinit var torProxyStateHandler: TorProxyStateHandler

    lateinit var transactionData: TransactionData

    val steps: MutableLiveData<List<FinalizingStep>> = MutableLiveData<List<FinalizingStep>>()
    val sentTxId: MutableLiveData<TxId> = MutableLiveData()
    val txFailureReason: MutableLiveData<TxFailureReason> = MutableLiveData()
    val isSuccess = MutableLiveData<Boolean>()
    val nextStep = MutableLiveData<FinalizingStep>()

    init {
        component.inject(this)
    }

    fun start() {
        steps.value = listOfNotNull(
            ConnectionCheckStep(),
            DiscoveryStep(),
            SentStep().takeIf { !transactionData.isOneSidePayment },
        )

        checkStepStatus()
    }

    fun checkStepStatus() {
        steps.value?.firstOrNull { !it.isCompleted || !it.isStarted }?.let {
            val isStarted = it.isStarted
            it.check()

            if (!isStarted) {
                this.nextStep.postValue(it)
                return
            }
        }

        if (steps.value?.all { it.isStarted && it.isCompleted } == true && txFailureReason.value == null) {
            isSuccess.postValue(true)
        }
    }

    fun onYatSendTxStop() {
        trySendTxSuccess(isYat = true)
        trySendTxSuccess(isYat = true)
    }

    fun trySendTxSuccess(isYat: Boolean) {
        sentTxId.value?.let {
            tariNavigator.navigate(Navigation.SendTxNavigation.OnSendTxSuccess(isYat = isYat, txId = it))
        }
    }

    fun trySendTxFailure(isYat: Boolean) {
        txFailureReason.value?.let {
            tariNavigator.navigate(Navigation.SendTxNavigation.OnSendTxFailure(isYat = isYat, txFailureReason = it))
        }
    }

    sealed class FinalizingStep(
        @StringRes val descLine1Res: Int,
        @StringRes val descLine2Res: Int,
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
                val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
                if (networkConnectionState != NetworkConnectionState.CONNECTED) {
                    // internet connection problem
                    txFailureReason.value = TxFailureReason.NETWORK_CONNECTION_ERROR
                } else {
                    // tor connection problem
                    txFailureReason.value = TxFailureReason.BASE_NODE_CONNECTION_ERROR
                }
                isCompleted = true
                return
            }
        }

        override fun execute() = doOnWalletServiceConnected { onServiceConnected() }

        private fun onServiceConnected() {
            // start checking network connection
            connectionCheckStartTime = DateTime.now()
            checkConnectionStatus()
        }

        private fun checkConnectionStatus() {
            val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
            val torProxyState = torProxyStateHandler.torProxyState.value
            // check internet connection
            if (networkConnectionState != NetworkConnectionState.CONNECTED) {
                // either not connected or Tor proxy is not running
                txFailureReason.value = TxFailureReason.NETWORK_CONNECTION_ERROR
                isCompleted = true
                return
            }
            // check whether Tor proxy is running
            if (torProxyState !is TorProxyState.Running) {
                // either not connected or Tor proxy is not running
                txFailureReason.value = TxFailureReason.NETWORK_CONNECTION_ERROR
                isCompleted = true
                return
            }
            // check Tor bootstrap status
            if (torProxyState.bootstrapStatus.progress < TorBootstrapStatus.MAX_PROGRESS) {
                launchOnIo {
                    torProxyStateHandler.doOnTorBootstrapped {
                        launchOnMain {
                            checkConnectionStatus()
                        }
                    }
                }
            } else {
                isCompleted = true
            }
        }
    }

    inner class DiscoveryStep : FinalizingStep(
        descLine1Res = if (transactionData.isOneSidePayment) finalize_send_tx_sending_step_3_desc_line_1 else finalize_send_tx_sending_step_2_desc_line_1,
        descLine2Res = if (transactionData.isOneSidePayment) finalize_send_tx_sending_step_3_desc_line_2 else finalize_send_tx_sending_step_2_desc_line_2,
    ) {
        override fun execute() {
            launchOnIo {
                val error = WalletError()
                val txId = walletService.sendTari(
                    /* contact = */ TariContact(transactionData.recipientContact!!.contactInfo.requireWalletAddress()),
                    /* amount = */ transactionData.amount,
                    /* feePerGram = */ transactionData.feePerGram ?: Constants.Wallet.DEFAULT_FEE_PER_GRAM,
                    /* message = */ transactionData.message,
                    /* isOneSidePayment = */ transactionData.isOneSidePayment,
                    /* paymentId = */ transactionData.paymentId,
                    /* error = */ error,
                )
                // if success, just wait for the callback to happen
                // if failed, just show the failed info & return
                isCompleted = true
                if (txId == null || error != WalletError.NoError) {
                    txFailureReason.postValue(TxFailureReason.SEND_ERROR)
                } else {
                    sentTxId.postValue(txId)
                }
            }
        }
    }

    inner class SentStep : FinalizingStep(
        descLine1Res = finalize_send_tx_sending_step_3_desc_line_1,
        descLine2Res = finalize_send_tx_sending_step_3_desc_line_2,
    ) {

        init {
            collectFlow(walletManager.walletEvent) { event ->
                when (event) {
                    is WalletManager.WalletEvent.Tx.DirectSendResult -> onDirectSendResult(event.txId, event.status)
                    else -> Unit
                }
            }
        }

        override fun execute() = Unit

        private fun onDirectSendResult(txId: TxId, status: TransactionSendStatus) {
            if (sentTxId.value != txId) {
                return
            }
            if (status.isSuccess) {
                // progress state
                finishSendingTx()
            }
        }

        private fun finishSendingTx() {
            EventBus.unsubscribe(this)
            isCompleted = true
        }
    }

    companion object {
        const val KEY_TRANSACTION_DATA = "Finalize_send_tx_fragment_key"
        const val CONNECTION_TIMEOUT_SEC = 30
    }
}