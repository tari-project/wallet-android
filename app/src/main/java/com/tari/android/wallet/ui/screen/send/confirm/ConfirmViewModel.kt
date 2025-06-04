package com.tari.android.wallet.ui.screen.send.confirm

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_TRANSACTION
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.shortString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConfirmViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            ticker = networkRepository.currentNetwork.ticker,
            transactionData = savedState.getOrThrow<TransactionData>(PARAMETER_TRANSACTION),
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onConfirmClicked() {
        tariNavigator.navigate(Navigation.TxSend.ToFinalizing(uiState.value.transactionData))
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
    ) {
        val screenTitle: String
            get() = transactionData.recipientContact.alias.orEmpty().takeIf { it.isNotBlank() }
                ?: transactionData.recipientContact.walletAddress.shortString()

        val totalAmount: MicroTari
            get() = transactionData.amount + transactionData.feePerGram
    }
}

