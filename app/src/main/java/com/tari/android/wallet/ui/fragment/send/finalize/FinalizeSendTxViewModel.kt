package com.tari.android.wallet.ui.fragment.send.finalize

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.WalletErrorCode
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.connection.TariWalletServiceConnection
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import javax.inject.Inject

class FinalizeSendTxViewModel : CommonViewModel() {

    lateinit var transactionData: TransactionData
    lateinit var connectionCheckStartTime: DateTime

    var currentStep: MutableLiveData<FinalizingStep> = MutableLiveData()
    var sentTxId: MutableLiveData<TxId> = MutableLiveData()
    var txFailureReason: MutableLiveData<TxFailureReason> = MutableLiveData()
    var torConnected: SingleLiveEvent<Unit> = SingleLiveEvent()

    /**
     * Both send methods have to fail to arrive at the judgement that the send has failed.
     */
    private var directSendHasFailed = false
    private var storeAndForwardHasFailed = false

    //todo need to get rid of it logic. Make it usual RX
    var switchToNextProgressStateOnProgressAnimComplete = false

    private val connectionService: TariWalletServiceConnection = TariWalletServiceConnection()
    val walletService: TariWalletService
        get() = connectionService.currentState.service!!

    @Inject
    lateinit var tracker: Tracker

    init {
        component.inject(this)

        currentStep.value = FinalizingStep.ConnectionCheck(resourceManager)

        connectionService.connection.subscribe {
            if (it.status == TariWalletServiceConnection.ServiceConnectionStatus.CONNECTED) onServiceConnected()
        }.addTo(compositeDisposable)

        tracker.screen(path = "/home/send_tari/finalize", title = "Send Tari - Finalize")
    }

    private fun onServiceConnected() {
        // start checking network connection
        connectionCheckStartTime = DateTime.now()
        checkConnectionStatus()
        subscribeToEventBus()
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Transaction.DirectSendResult>(this) { event -> onDirectSendResult(event.txId, event.success) }
        EventBus.subscribe<Event.Transaction.StoreAndForwardSendResult>(this) { event -> onStoreAndForwardSendResult(event.txId, event.success) }
    }

    /**
     * Step #1 of the flow.
     */
    private fun checkConnectionStatus() {
        val networkConnectionState = EventBus.networkConnectionState.publishSubject.value
        val torProxyState = EventBus.torProxyState.publishSubject.value
        // check internet connection
        if (networkConnectionState != NetworkConnectionState.CONNECTED) {
            Logger.w("Send error: not connected to the internet.")
            // either not connected or Tor proxy is not running
            txFailureReason.postValue(TxFailureReason.NETWORK_CONNECTION_ERROR)
            return
        }
        // check whether Tor proxy is running
        if (torProxyState !is TorProxyState.Running) {
            Logger.w("Send error: Tor proxy is not running.")
            // either not connected or Tor proxy is not running
            txFailureReason.postValue(TxFailureReason.NETWORK_CONNECTION_ERROR)
            return
        }
        // check Tor bootstrap status
        if (torProxyState.bootstrapStatus.progress < TorBootstrapStatus.maxProgress) {
            Logger.d("Tor proxy not ready - start waiting.")
            // subscribe to Tor proxy state changes - start waiting on it
            EventBus.torProxyState.publishSubject.subscribe { state ->
                onTorProxyStateChanged(state)
            }.addTo(compositeDisposable)
        } else {
            torConnected.postValue(Unit)
            switchToNextProgressStateOnProgressAnimComplete = true
        }
    }

    private fun onTorProxyStateChanged(torProxyState: TorProxyState) {
        if (torProxyState is TorProxyState.Running) {
            if (torProxyState.bootstrapStatus.progress == TorBootstrapStatus.maxProgress) {
                EventBus.torProxyState.unsubscribe(this)
                checkConnectionStatus()
            }
        }
    }

    /**
     * Start step 2 and 3
     */
    fun sendTari() {
        viewModelScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val txId = walletService.sendTari(
                transactionData.recipientUser,
                transactionData.amount,
                Constants.Wallet.defaultFeePerGram,
                transactionData.note,
                error
            )
            // if success, just wait for the callback to happen
            // if failed, just show the failed info & return
            if (txId == null || error.code != WalletErrorCode.NO_ERROR) {
                txFailureReason.postValue(TxFailureReason.SEND_ERROR)
            } else {
                sentTxId.postValue(txId)
            }
        }
    }

    /**
     * Step #2 and 3 altogether of the flow.
     */
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
            switchToNextProgressStateOnProgressAnimComplete = true
            EventBus.unsubscribe(this)
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
            switchToNextProgressStateOnProgressAnimComplete = true
            EventBus.unsubscribe(this)
        } else {
            storeAndForwardHasFailed = true
            checkForCombinedFailure()
        }
    }

    private fun checkForCombinedFailure() {
        if (directSendHasFailed && storeAndForwardHasFailed) { // both have failed
            txFailureReason.postValue(TxFailureReason.SEND_ERROR)
        }
    }

    companion object {
        const val transactionDataKey = "Finalize_send_tx_fragment_key"
    }
}