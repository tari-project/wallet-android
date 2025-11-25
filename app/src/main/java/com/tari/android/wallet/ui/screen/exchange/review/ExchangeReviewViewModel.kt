package com.tari.android.wallet.ui.screen.exchange.review

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent.TxSend.TxSendFailed.TxFailureReason
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.exchange.review.ExchangeReviewFragment.Companion.ARG_REQUEST
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.switchToMain
import com.tari.android.wallet.util.extension.toMicroTari
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ExchangeReviewViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        savedState.getOrThrow<ExchangeRequestData>(ARG_REQUEST).let { request ->
            UiState(
                request = request,
                walletAddress = sharedPrefsRepository.walletAddress,
                fee = walletManager.requireWalletInstance.estimateTxFee(request.amount.toMicroTari()),
            )
        }
    )
    val uiState = _uiState.asStateFlow()

    init {
        createExchange()
    }

    fun onRetry() {
        createExchange()
    }

    fun onConfirmClicked() {
        val transaction = _uiState.value.transaction ?: error("Exchange transaction is for some reason null on confirm")
        val depositAddress = transaction.depositAddress
        val amount = _uiState.value.request.amount

        _uiState.update { it.copy(sending = true) }

        launchOnIo {
            // First check network connection
            if (!networkConnection.isNetworkConnected()) {
                walletManager.sendWalletEvent(WalletManager.WalletEvent.TxSend.TxSendFailed(TxFailureReason.NETWORK_CONNECTION_ERROR))
                switchToMain { _uiState.update { it.copy(sending = false) } }
                return@launchOnIo
            }

            // Parse the deposit address
            val tariAddress = runCatching {
                TariWalletAddress.fromBase58(depositAddress)
            }.getOrElse {
                logger.e("Failed to parse deposit address: $depositAddress", it)
                switchToMain {
                    _uiState.update { state -> state.copy(sending = false) }
                    showSimpleDialog(
                        title = resourceManager.getString(R.string.common_error_title),
                        description = resourceManager.getString(R.string.exchange_invalid_deposit_address_error),
                    )
                }
                return@launchOnIo
            }

            // Send the transaction
            runCatching {
                val txId = walletManager.sendTari(
                    tariContact = TariContact(tariAddress),
                    amount = amount.toMicroTari(),
                    message = resourceManager.getString(R.string.exchange_tx_message),
                )

                logger.i("Exchange tx sent: $txId")

                switchToMain {
                    tariNavigator.navigateSequence(
                        Navigation.BackToHome,
                        Navigation.Exchange.ExchangeStatus(transaction.id),
                    )
                }
            }.onFailure { exception ->
                logger.d("Failed to send exchange tx: ${exception.message}")
                switchToMain { _uiState.update { it.copy(sending = false) } }
                showSimpleDialog(
                    title = resourceManager.getString(R.string.common_error_title),
                    description = resourceManager.getString(R.string.exchange_problem_sending_tx),
                )
            }
        }
    }

    fun copyValueToClipboard(value: String) {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.exchange_review_label),
            clipText = value,
        )
    }

    fun onAddressDetailsClicked() {
        showAddressDetailsDialog(_uiState.value.walletAddress)
    }

    fun onFeeInfoClicked() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.tx_detail_fee_tooltip_transaction_fee),
            description = resourceManager.getString(R.string.tx_detail_fee_tooltip_desc),
        )
    }

    private fun createExchange() {
        _uiState.update { it.copy(creatingExchange = true, creatingExchangeError = null) }
        launchOnIo {
            exolixRepository.createExchange(_uiState.value.request)
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            creatingExchange = false,
                            transaction = response,
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            creatingExchange = false,
                            creatingExchangeError = exception.message ?: resourceManager.getString(R.string.exchange_create_exchange_error),
                        )
                    }
                }
        }
    }

    data class UiState(
        val request: ExchangeRequestData,
        val walletAddress: TariWalletAddress,
        val fee: MicroTari,
        val transaction: Exolix.Transaction? = null,

        val creatingExchange: Boolean = false,
        val creatingExchangeError: String? = null,

        val sending: Boolean = false,
    )
}
