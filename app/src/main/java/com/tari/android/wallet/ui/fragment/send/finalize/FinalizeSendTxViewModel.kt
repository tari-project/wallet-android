package com.tari.android.wallet.ui.fragment.send.finalize

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.Seconds
import javax.inject.Inject

class FinalizeSendTxViewModel : CommonViewModel() {

    lateinit var transactionData: TransactionData

    val steps: MutableLiveData<MutableList<FinalizingStep>> = MutableLiveData<MutableList<FinalizingStep>>()
    val sentTxId: MutableLiveData<TxId> = MutableLiveData()
    val txFailureReason: MutableLiveData<TxFailureReason> = MutableLiveData()
    val isSuccess = MutableLiveData<Boolean>()
    val nextStep = MutableLiveData<FinalizingStep>()

    private val connectionService: TariWalletServiceConnection = TariWalletServiceConnection()
    val walletService: TariWalletService
        get() = connectionService.currentState.service!!

    init {
        component.inject(this)

        tracker.screen(path = "/home/send_tari/finalize", title = "Send Tari - Finalize")
    }

    fun start() {
        val stepList = mutableListOf<FinalizingStep>()
        stepList.add(ConnectionCheckStep(resourceManager))
        stepList.add(DiscoveryStep(resourceManager))
        if (!transactionData.isOneSidePayment) {
            stepList.add(SentStep(resourceManager))
        }
        steps.value = stepList

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

    abstract inner class FinalizingStep(val resourceManager: ResourceManager, descLine1Res: Int, descLine2Res: Int) {

        var isStarted: Boolean = false
        var isCompleted: Boolean = false

        var descriptionLine1 = resourceManager.getString(descLine1Res)
        var descriptionLine2 = resourceManager.getString(descLine2Res)

        abstract fun execute()

        open fun check() {
            if (!isStarted) {
                isStarted = true
                execute()
            }
        }
    }

    inner class ConnectionCheckStep(resourceManager: ResourceManager) : FinalizingStep(
        resourceManager,
        finalize_send_tx_sending_step_1_desc_line_1,
        finalize_send_tx_sending_step_1_desc_line_2
    ) {
        var connectionCheckStartTime: DateTime? = null

        override fun check() {
            super.check()

            connectionCheckStartTime ?: return

            val secondsElapsed = Seconds.secondsBetween(connectionCheckStartTime, DateTime.now()).seconds
            if (secondsElapsed >= connectionTimeoutSecs) {
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

        override fun execute() {
            connectionService.connection.subscribe {
                if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) onServiceConnected()
            }.addTo(compositeDisposable)
        }

        private fun onServiceConnected() {
            // start checking network connection
            connectionCheckStartTime = DateTime.now()
            checkConnectionStatus()
        }

        private fun onTorProxyStateChanged(torProxyState: TorProxyState) {
            if (torProxyState is TorProxyState.Running) {
                if (torProxyState.bootstrapStatus.progress == TorBootstrapStatus.maxProgress) {
                    EventBus.torProxyState.unsubscribe(this)
                    checkConnectionStatus()
                }
            }
        }

        private fun checkConnectionStatus() {
            val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
            val torProxyState = EventBus.torProxyState.publishSubject.value
            // check internet connection
            if (networkConnectionState != NetworkConnectionState.CONNECTED) {
                Logger.w("Send error: not connected to the internet.")
                // either not connected or Tor proxy is not running
                txFailureReason.value = TxFailureReason.NETWORK_CONNECTION_ERROR
                isCompleted = true
                return
            }
            // check whether Tor proxy is running
            if (torProxyState !is TorProxyState.Running) {
                Logger.w("Send error: Tor proxy is not running.")
                // either not connected or Tor proxy is not running
                txFailureReason.value = TxFailureReason.NETWORK_CONNECTION_ERROR
                isCompleted = true
                return
            }
            // check Tor bootstrap status
            if (torProxyState.bootstrapStatus.progress < TorBootstrapStatus.maxProgress) {
                Logger.d("Tor proxy not ready - start waiting.")
                // subscribe to Tor proxy state changes - start waiting on it
                EventBus.torProxyState.publishSubject.subscribe { state -> onTorProxyStateChanged(state) }.addTo(compositeDisposable)
            } else {
                isCompleted = true
            }
        }
    }

    open inner class DiscoveryStep(resourceManager: ResourceManager) :
        FinalizingStep(
            resourceManager,
            finalize_send_tx_sending_step_2_desc_line_1,
            finalize_send_tx_sending_step_2_desc_line_2
        ) {

        init {
            if (transactionData.isOneSidePayment) {
                descriptionLine1 = resourceManager.getString(finalize_send_tx_sending_step_3_desc_line_1)
                descriptionLine2 = resourceManager.getString(finalize_send_tx_sending_step_3_desc_line_2)
            }
        }

        override fun execute() {
            viewModelScope.launch(Dispatchers.IO) {
                val error = WalletError()
                val txId = walletService.sendTari(
                    transactionData.recipientUser,
                    transactionData.amount,
                    Constants.Wallet.defaultFeePerGram,
                    transactionData.note,
                    transactionData.isOneSidePayment,
                    error
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

    inner class SentStep(resourceManager: ResourceManager) :
        FinalizingStep(
            resourceManager,
            finalize_send_tx_sending_step_3_desc_line_1,
            finalize_send_tx_sending_step_3_desc_line_2
        ) {

        init {
            subscribeToEventBus()
        }

        /**
         * Both send methods have to fail to arrive at the judgement that the send has failed.
         */
        private var directSendHasFailed = false
        private var storeAndForwardHasFailed = false

        override fun execute() = Unit

        private fun subscribeToEventBus() {
            EventBus.subscribe<Event.Transaction.DirectSendResult>(this) { event -> onDirectSendResult(event.txId, event.success) }
            EventBus.subscribe<Event.Transaction.StoreAndForwardSendResult>(this) { event -> onStoreAndForwardSendResult(event.txId, event.success) }
        }

        private fun onDirectSendResult(txId: TxId, success: Boolean) {
            if (sentTxId.value != txId) {
                Logger.d("Response received for another tx with id: ${txId.value}.")
                return
            }
            Logger.d("Direct Send completed with result: $success.")
            if (success) {
                // track event
                tracker.event(category = "Transaction", action = "Transaction Accepted - Synchronous")
                // progress state
                finishSendingTx()
            } else {
                directSendHasFailed = true
                checkForCombinedFailure()
            }
        }

        private fun onStoreAndForwardSendResult(txId: TxId, success: Boolean) {
            if (sentTxId.value != txId) {
                Logger.d("Response received for another tx with id: ${txId.value}.")
                return
            }
            Logger.d("Store and forward send completed with result: $success.")
            if (success) {
                // track event
                tracker.event(category = "Transaction", action = "Transaction Stored")
                // progress state
                finishSendingTx()
            } else {
                storeAndForwardHasFailed = true
                checkForCombinedFailure()
            }
        }

        private fun finishSendingTx() {
            EventBus.unsubscribe(this)
            isCompleted = true
        }

        private fun checkForCombinedFailure() {
            if (directSendHasFailed && storeAndForwardHasFailed) { // both have failed
                txFailureReason.value = TxFailureReason.SEND_ERROR
                isCompleted = true
            }
        }
    }

    companion object {
        const val transactionDataKey = "Finalize_send_tx_fragment_key"
        const val connectionTimeoutSecs = 30
    }
}