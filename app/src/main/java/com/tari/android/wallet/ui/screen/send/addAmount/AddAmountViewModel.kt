package com.tari.android.wallet.ui.screen.send.addAmount

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_AMOUNT
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_NOTE
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.screen.send.addAmount.feeModule.FeeModule
import com.tari.android.wallet.ui.screen.send.addAmount.feeModule.NetworkSpeed
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.getOrNull
import com.tari.android.wallet.util.extension.getWithError
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.toMicroTari
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AddAmountViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    var selectedFeeData: FeeData? = null
    private var selectedSpeed: NetworkSpeed = NetworkSpeed.Medium

    private var feeData: List<FeeData> = listOf()

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        savedState.get<ContactDto>(PARAMETER_CONTACT)?.getFFIContactInfo()?.let { contact ->
            AddAmountModel.UiState(
                amount = savedState.get<MicroTari>(PARAMETER_AMOUNT)?.tariValue?.toDouble() ?: Double.MIN_VALUE,
                recipientContactInfo = contact,
                note = savedState.get<String>(PARAMETER_NOTE).orEmpty(),
            )
        } ?: error("FFI contact is required, but not provided (maybe it is a PhoneContactInfo which does not have a wallet address).")
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
        launchOnIo {
            try {
                _uiState.update {
                    it.copy(feePerGrams = wallet.getFeePerGramStats())
                }
            } catch (e: Throwable) {
                logger.i("Error loading fees: ${e.message}")
            }
        }
    }

    fun showFeeDialog() {
        val feeModule = FeeModule(0.toMicroTari(), feeData, selectedSpeed)
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.add_amount_modify_fee_title)),
            BodyModule(resourceManager.getString(R.string.add_amount_modify_fee_description)),
            feeModule,
            ButtonModule(resourceManager.getString(R.string.add_amount_modify_fee_use), ButtonStyle.Normal) {
                selectedSpeed = feeModule.selectedSpeed
                selectedFeeData = feeModule.feePerGram
                hideDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
    }

    fun emojiIdClicked(walletAddress: TariWalletAddress) {
        showAddressDetailsDialog(walletAddress)
    }

    fun calculateFee(amount: MicroTari) {
        val grams = uiState.value.feePerGrams
        if (grams == null) {
            calculateDefaultFees(amount)
            return
        }

        val wallet = walletManager.requireWalletInstance

        val slowFee = wallet.getWithError(this::showFeeError) { it.estimateTxFee(amount, grams.slow) }
        val mediumFee = wallet.getWithError(this::showFeeError) { it.estimateTxFee(amount, grams.medium) }
        val fastFee = wallet.getWithError(this::showFeeError) { it.estimateTxFee(amount, grams.fast) }

        if (slowFee == null || mediumFee == null || fastFee == null) {
            calculateDefaultFees(amount)
            return
        }

        feeData = listOf(FeeData(grams.slow, slowFee), FeeData(grams.medium, mediumFee), FeeData(grams.fast, fastFee))
        selectedFeeData = feeData[1]
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
        selectedFeeData = FeeData(Constants.Wallet.DEFAULT_FEE_PER_GRAM, calculatedFee)
    }

    private fun showFeeError(walletError: WalletError) {
        if (walletError != WalletError.NoError) {
            showFeeError()
        }
    }

    private fun showFeeError() = Unit
}