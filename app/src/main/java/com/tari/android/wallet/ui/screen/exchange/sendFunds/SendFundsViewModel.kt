package com.tari.android.wallet.ui.screen.exchange.sendFunds

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.exchange.sendFunds.SendFundsFragment.Companion.ARG_REQUEST
import com.tari.android.wallet.ui.screen.exchange.sendFunds.SendFundsFragment.Companion.ARG_TRANSACTION
import com.tari.android.wallet.util.QrUtil
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class SendFundsViewModel(val savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(transaction = savedState.get<Exolix.Transaction>(ARG_TRANSACTION))
    )
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.value.transaction?.let {
            initializeFromTransaction(it)
        } ?: run {
            loadTransaction()
        }
    }

    private fun loadTransaction() {
        val request = savedState.getOrThrow<ExchangeRequestData>(ARG_REQUEST)

        _uiState.update { it.copy(loading = true, error = null) }
        launchOnIo {
            exolixRepository.createExchange(request)
                .onSuccess { transaction ->
                    initializeFromTransaction(transaction)
                    _uiState.update { it.copy(loading = false, transaction = transaction) }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = exception.message ?: resourceManager.getString(R.string.exchange_create_exchange_error),
                        )
                    }
                }
        }
    }

    private fun initializeFromTransaction(transaction: Exolix.Transaction) {
        _uiState.update {
            it.copy(
                qrBitmap = QrUtil.getQrEncodedBitmapOrNull(
                    content = transaction.depositAddress,
                    size = resourceManager.getDimenInPx(R.dimen.wallet_info_img_qr_code_size),
                )
            )
        }
    }

    fun onRetry() {
        loadTransaction()
    }

    fun onCopyAmount() {
        _uiState.value.transaction?.let { transaction ->
            copyToClipboard(
                clipLabel = resourceManager.getString(R.string.exchange_amount_label),
                clipText = transaction.amount.toString(),
                toastMessage = resourceManager.getString(R.string.exchange_amount_copied),
            )
        }
    }

    fun onCopyAddress() {
        _uiState.value.transaction?.let { transaction ->
            copyToClipboard(
                clipLabel = resourceManager.getString(R.string.exchange_address_label),
                clipText = transaction.depositAddress,
                toastMessage = resourceManager.getString(R.string.exchange_address_copied),
            )
        }
    }

    fun onOpenTxDetails() {
        // TODO: Implement when transaction details screen is ready
        _uiState.value.transaction?.let { transaction ->
            tariNavigator.navigateSequence(
                Navigation.BackToHome,
                Navigation.Exchange.ExchangeStatus(transaction.id),
            )
        }
    }

    data class UiState(
        val transaction: Exolix.Transaction? = null,

        val qrBitmap: Bitmap? = null,

        val loading: Boolean = false,
        val error: String? = null,
    )
}
