package com.tari.android.wallet.ui.fragment.send.addAmount

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_AMOUNT
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_NOTE
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.FeeModule
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.NetworkSpeed
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigInteger
import kotlin.math.min

class AddAmountViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    var selectedFeeData: FeeData? = null
    private var selectedSpeed: NetworkSpeed = NetworkSpeed.Medium

    private var feeData: List<FeeData> = listOf()

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        savedState.get<ContactDto>(PARAMETER_CONTACT)?.let { contact ->
            AddAmountModel.UiState(
                isOneSidedPaymentEnabled = tariSettingsSharedRepository.isOneSidePaymentEnabled,
                isOneSidedPaymentForced = contact.contactInfo.requireWalletAddress().oneSided && !contact.contactInfo.requireWalletAddress().interactive,
                amount = savedState.get<MicroTari>(PARAMETER_AMOUNT)?.tariValue?.toDouble() ?: Double.MIN_VALUE,
                contactDto = contact,
                note = savedState.get<String>(PARAMETER_NOTE).orEmpty(),
            )
        } ?: error("Contact is required, but not provided")
    )
    val uiState = _uiState.asStateFlow()

    init {
        doOnWalletServiceConnected { _uiState.update { it.copy(serviceConnected = true) } }

        loadFees()
    }

    private fun loadFees() = doOnWalletServiceConnected {
        launchOnIo {
            try {
                val stats = walletManager.walletInstance!!.getFeePerGramStats()
                val elements = (0 until stats.getLength()).map { stats.getAt(it) }.toList()

                val elementsCount = min(stats.getLength(), 3)
                val slowOption: BigInteger
                val mediumOption: BigInteger
                val fastOption: BigInteger
                val networkSpeed: NetworkSpeed

                when (elementsCount) {
                    1 -> {
                        networkSpeed = NetworkSpeed.Slow
                        slowOption = elements[0].getMin()
                        mediumOption = elements[0].getAverage()
                        fastOption = elements[0].getMax()
                    }

                    2 -> {
                        networkSpeed = NetworkSpeed.Medium
                        slowOption = elements[1].getAverage()
                        mediumOption = elements[0].getMin()
                        fastOption = elements[0].getMax()
                    }

                    3 -> {
                        networkSpeed = NetworkSpeed.Fast
                        slowOption = elements[2].getAverage()
                        mediumOption = elements[1].getAverage()
                        fastOption = elements[0].getMax()
                    }

                    else -> throw Exception("Unexpected block count")
                }
                _uiState.update {
                    it.copy(
                        feePerGrams = FeePerGramOptions(
                            networkSpeed = networkSpeed,
                            slow = MicroTari(slowOption),
                            medium = MicroTari(mediumOption),
                            fast = MicroTari(fastOption),
                        )
                    )
                }
            } catch (e: Throwable) {
                logger.i("Error loading fees: ${e.message}")
            }
        }
    }

    fun toggleOneSidePayment() {
        val newValue = !tariSettingsSharedRepository.isOneSidePaymentEnabled
        tariSettingsSharedRepository.isOneSidePaymentEnabled = newValue
    }

    fun showFeeDialog() {
        val feeModule = FeeModule(MicroTari(), feeData, selectedSpeed)
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

    fun calculateFee(amount: MicroTari, walletError: WalletError) {
        try {
            val grams = uiState.value.feePerGrams
            if (grams == null) {
                calculateDefaultFees(amount, walletError)
                return
            }

            val slowFee = walletService.getWithError(this::showFeeError) { _, wallet -> wallet.estimateTxFee(amount, walletError, grams.slow) }
            val mediumFee =
                walletService.getWithError(this::showFeeError) { _, wallet -> wallet.estimateTxFee(amount, walletError, grams.medium) }
            val fastFee = walletService.getWithError(this::showFeeError) { _, wallet -> wallet.estimateTxFee(amount, walletError, grams.fast) }

            if (slowFee == null || mediumFee == null || fastFee == null) {
                calculateDefaultFees(amount, walletError)
                return
            }

            feeData = listOf(FeeData(grams.slow, slowFee), FeeData(grams.medium, mediumFee), FeeData(grams.fast, fastFee))
            selectedFeeData = feeData[1]
        } catch (e: Throwable) {
            logger.i(e.message + "calculate fees")
        }
    }

    private fun calculateDefaultFees(amount: MicroTari, walletError: WalletError) {
        val calculatedFee = walletService.getWithError(this::showFeeError) { _, wallet ->
            wallet.estimateTxFee(amount, walletError, Constants.Wallet.DEFAULT_FEE_PER_GRAM)
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