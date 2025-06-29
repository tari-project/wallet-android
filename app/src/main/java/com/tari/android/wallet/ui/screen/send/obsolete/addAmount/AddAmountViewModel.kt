package com.tari.android.wallet.ui.screen.send.obsolete.addAmount

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_AMOUNT
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_NOTE
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.getOrNull
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.getWithError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AddAmountViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    var feeData: FeeData? = null
    val feeDataRequired
        get() = feeData ?: error("Fee data is required but not set. Please call calculateFee() first.")

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        AddAmountModel.UiState(
            amount = savedState.get<MicroTari>(PARAMETER_AMOUNT)?.tariValue?.toDouble() ?: Double.MIN_VALUE,
            recipientContact = savedState.getOrThrow<Contact>(PARAMETER_CONTACT),
            note = savedState.get<String>(PARAMETER_NOTE).orEmpty(),
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<AddAmountModel.Effect>()
    val effect = _effect.flow

    val walletBalance: BalanceInfo
        get() = walletManager.requireWalletInstance.getBalance()

    init {
        doOnWalletRunning { _effect.send(AddAmountModel.Effect.SetupUi(uiState.value)) }

        loadFees()
    }

    private fun loadFees() = doOnWalletRunning { wallet ->
        runCatching {
            _uiState.update { it.copy(feePerGram = wallet.getLowestFeePerGram()) }
        }.onFailure { logger.i("Error loading fees: ${it.message}") }
    }

    fun emojiIdClicked(walletAddress: TariWalletAddress) {
        showAddressDetailsDialog(walletAddress)
    }

    fun calculateFee(amount: MicroTari) {
        val feePerGram = uiState.value.feePerGram
        if (feePerGram == null) {
            calculateDefaultFees(amount)
            return
        }

        val fee = walletManager.requireWalletInstance.getWithError(this::showFeeError) { it.estimateTxFee(amount, feePerGram) }
        if (fee == null) {
            calculateDefaultFees(amount)
            return
        }

        feeData = FeeData(feePerGram, fee)
    }

    fun continueToAddNote(transactionData: TransactionData) {
        if (!networkConnection.isNetworkConnected()) {
            showInternetConnectionErrorDialog()
        } else if (transactionData.shouldSkipAddingNote()) {
            tariNavigator.navigate(Navigation.TxSend.ToConfirm(transactionData))
        } else {
            tariNavigator.navigate(Navigation.TxSend.ToAddNote(transactionData))
        }
    }

    fun showAmountExceededError() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.error_balance_exceeded_title),
            description = resourceManager.getString(R.string.error_balance_exceeded_description),
        )
    }

    fun showTxFeeToolTip() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.tx_detail_fee_tooltip_transaction_fee),
            description = resourceManager.getString(R.string.tx_detail_fee_tooltip_desc),
        )
    }

    private fun TransactionData.shouldSkipAddingNote() = note.isNullOrEmpty() || DebugConfig.skipAddingNote

    private fun calculateDefaultFees(amount: MicroTari) {
        val calculatedFee = walletManager.requireWalletInstance.getOrNull { wallet ->
            wallet.estimateTxFee(amount, Constants.Wallet.DEFAULT_FEE_PER_GRAM)
        } ?: return
        feeData = FeeData(Constants.Wallet.DEFAULT_FEE_PER_GRAM, calculatedFee)
    }

    private fun showFeeError(walletError: WalletError) {
        if (walletError != WalletError.NoError) {
            showFeeError()
        }
    }

    private fun showFeeError() = Unit

    data class FeeData(
        val feePerGram: MicroTari,
        val calculatedFee: MicroTari,
    )
}