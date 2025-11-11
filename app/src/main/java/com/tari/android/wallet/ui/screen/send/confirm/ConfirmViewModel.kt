package com.tari.android.wallet.ui.screen.send.confirm

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent.TxSend.TxSendFailed.TxFailureReason
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.send.confirm.ConfirmFragment.Companion.PARAMETER_TRANSACTION
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.switchToMain
import com.tari.android.wallet.util.shortString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ConfirmViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            ticker = networkRepository.currentNetwork.ticker,
            transactionData = savedState.getOrThrow(PARAMETER_TRANSACTION),
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onConfirmClicked() {
        _uiState.update { it.copy(isSending = true) }

        launchOnIo {
            // First check network connection
            if (!networkConnection.isNetworkConnected()) {
                walletManager.sendWalletEvent(WalletManager.WalletEvent.TxSend.TxSendFailed(TxFailureReason.NETWORK_CONNECTION_ERROR))
                switchToMain { _uiState.update { it.copy(isSending = false) } }
                return@launchOnIo
            }

            // Then send the transaction
            runCatching {
                val txId = walletManager.sendTari(
                    tariContact = TariContact(uiState.value.transactionData.recipientContact.walletAddress),
                    amount = uiState.value.transactionData.amount,
                    message = uiState.value.transactionData.message,
                )

                logger.i("Tx sent: $txId")
                walletManager.sendWalletEvent(WalletManager.WalletEvent.TxSend.TxSendSuccessful(txId))
            }.onFailure {
                walletManager.sendWalletEvent(WalletManager.WalletEvent.TxSend.TxSendFailed(TxFailureReason.SEND_ERROR))
            }

            switchToMain { tariNavigator.navigate(Navigation.BackToHome) }
        }
    }

    fun copyTxValueToClipboard(value: String) {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.tx_details_transaction_details),
            clipText = value,
        )
    }

    fun onFeeInfoClicked() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.tx_detail_fee_tooltip_transaction_fee),
            description = resourceManager.getString(R.string.tx_detail_fee_tooltip_desc),
        )
    }

    fun onAddressDetailsClicked() {
        showAddressDetailsDialog(uiState.value.transactionData.recipientContact.walletAddress)
    }

    data class UiState(
        val ticker: String,
        val transactionData: TransactionData,
        val isSending: Boolean = false,
    ) {
        val screenTitle: String
            get() = transactionData.recipientContact.alias.orEmpty().takeIf { it.isNotBlank() }
                ?: transactionData.recipientContact.walletAddress.shortString()

        val totalAmount: MicroTari
            get() = transactionData.amount + transactionData.fee
    }

}

