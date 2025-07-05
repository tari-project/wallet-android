package com.tari.android.wallet.ui.screen.send.send

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.addressPoisoning.AddressPoisoningChecker
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.BalanceStateHandler
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.AddressPoisoningModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.screen.send.send.SendFragment.Companion.PARAMETER_AMOUNT
import com.tari.android.wallet.ui.screen.send.send.SendFragment.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.screen.send.send.SendFragment.Companion.PARAMETER_NOTE
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.greaterThan
import com.tari.android.wallet.util.extension.isTrue
import com.tari.android.wallet.util.extension.launchOnMain
import com.tari.android.wallet.util.extension.letNotNull
import com.tari.android.wallet.util.extension.toMicroTari
import com.tari.android.wallet.util.extension.toMicroTariOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class SendViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var balanceStateHandler: BalanceStateHandler

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    @Inject
    lateinit var addressPoisoningChecker: AddressPoisoningChecker

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            contact = savedState.get<Contact>(PARAMETER_CONTACT),
            amountValue = savedState.get<MicroTari>(PARAMETER_AMOUNT)?.formattedTariValue.orEmpty(),
            note = savedState.get<String>(PARAMETER_NOTE).orEmpty(),
            ticker = networkRepository.currentNetwork.ticker,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        doOnWalletRunning { wallet ->
            _uiState.update { it.copy(feePerGram = wallet.getLowestFeePerGram()) }
        }

        collectFlow(balanceStateHandler.balanceState) { balanceState ->
            _uiState.update { it.copy(availableBalance = balanceState.availableBalance) }
        }
    }

    override fun handleDeeplink(deeplink: DeepLink) {
        deeplink.getTariAddressOrNull()?.let { onAddressChange(it) }
        deeplink.getAmountOrNull()?.let { onAmountChange(it.formattedTariValue) }
    }

    fun onAddressChange(addressValue: String) {
        TariWalletAddress.makeTariAddressOrNull(addressValue).let { address ->
            _uiState.update {
                it.copy(
                    contact = address?.let { address -> contactsRepository.findOrCreateContact(address) },
                    isContactAddressValid = addressValue.isBlank() || address != null,
                )
            }

            checkAddressPoisoning(address)
        }
    }

    fun selectContact(selectedContact: Contact) {
        _uiState.update {
            it.copy(
                contact = selectedContact,
                isContactAddressValid = true,
            )
        }
    }

    fun onAmountChange(amountValue: String) {
        _uiState.update { it.copy(amountValue = amountValue.replace(",", ".")) }

        _uiState.value.amount?.let { amount ->
            doOnWalletRunning { wallet ->
                val fee = runCatching { wallet.estimateTxFee(amount, uiState.value.feePerGram) }
                _uiState.update {
                    it.copy(
                        fee = fee.getOrNull(),
                        feeError = fee.exceptionOrNull()?.let { error ->
                            logger.i("Error estimating fee: ${error.message}")
                            when {
                                WalletError(error) == WalletError.FundsPendingError -> R.string.send_amount_field_error_fee_funds_pending
                                else -> R.string.send_amount_field_error_fee_default
                            }
                        },
                    )
                }
            }
        } ?: run {
            _uiState.update { it.copy(fee = null, feeError = null) }
        }
    }

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    fun onFeeHelpClicked() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.tx_detail_fee_tooltip_transaction_fee),
            description = resourceManager.getString(R.string.tx_detail_fee_tooltip_desc),
        )
    }

    fun onContactBookClick() {
        tariNavigator.navigate(Navigation.ContactBook.AllContacts(startForSelectResult = true))
    }

    fun onContinueClick() {
        if (uiState.value.contact?.walletAddress == corePrefRepository.walletAddress) {
            showCantSendYourselfDialog()
        } else if (!networkConnection.isNetworkConnected()) {
            showInternetConnectionErrorDialog()
        } else {
            uiState.value.transactionData?.let { tariNavigator.navigate(Navigation.TxSend.Confirm(it)) }
                ?: logger.i("Transaction data is null, cannot continue.")
        }
    }

    private fun checkAddressPoisoning(address: TariWalletAddress?) {
        addressPoisoningChecker.doOnAddressPoisoned(address) { addresses ->
            val addressPoisoningModule = AddressPoisoningModule(addresses)
            val continueButtonModule = ButtonModule(
                text = resourceManager.getString(R.string.common_continue),
                style = ButtonStyle.Normal,
                action = {
                    addressPoisoningModule.selectedAddress.contact.walletAddress.let { selectedAddress ->
                        addressPoisoningChecker.markAsTrusted(selectedAddress, addressPoisoningModule.markAsTrusted)
                        launchOnMain { hideDialog() }
                    }
                }
            )
            val cancelButtonModule = ButtonModule(
                text = resourceManager.getString(R.string.common_cancel),
                style = ButtonStyle.Close,
            )

            showModularDialog(
                addressPoisoningModule,
                continueButtonModule,
                cancelButtonModule,
            )
        }
    }

    private fun showCantSendYourselfDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.contact_book_select_contact_cant_send_to_yourself_title),
            description = resourceManager.getString(R.string.contact_book_select_contact_cant_send_to_yourself_description),
        )
    }

    data class UiState(
        val ticker: String,

        val contact: Contact? = null,
        val isContactAddressValid: Boolean = true,

        val amountValue: String = "",

        val feePerGram: MicroTari = Constants.Wallet.DEFAULT_FEE_PER_GRAM,
        val fee: MicroTari? = null,
        @param:StringRes val feeError: Int? = null,

        val note: String = "",

        val availableBalance: MicroTari? = null,
    ) {
        val amount: MicroTari?
            get() = amountValue.toMicroTariOrNull()
        val isAmountValid: Boolean
            get() = amountValue.isBlank() || amount != null

        val continueButtonEnabled: Boolean
            get() = transactionData != null

        val transactionData: TransactionData?
            get() = letNotNull(contact, amount.takeIf { amountError == null }) { contact, amount ->
                TransactionData(
                    recipientContact = contact,
                    amount = amount,
                    note = note,
                    feePerGram = feePerGram,
                )
            }

        val amountError: Int?
            @StringRes get() = when {
                !isAmountValid -> R.string.send_amount_field_error
                availableBalanceError -> R.string.send_amount_field_error_not_enough_funds
                fee.greaterThan(amount) -> R.string.send_amount_field_error_lower_than_fee
                feeError != null -> feeError
                else -> null
            }

        val availableBalanceError: Boolean
            get() = (amount?.plus(fee ?: 0.toMicroTari())).greaterThan(availableBalance)

        val disabledNoteField: Boolean
            get() = contact?.walletAddress?.paymentIdAddress.isTrue()
    }
}
