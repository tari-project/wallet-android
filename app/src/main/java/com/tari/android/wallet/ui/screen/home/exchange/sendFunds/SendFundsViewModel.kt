package com.tari.android.wallet.ui.screen.home.exchange.sendFunds

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.home.exchange.sendFunds.SendFundsFragment.Companion.ARG_REQUEST
import com.tari.android.wallet.util.QrUtil
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class SendFundsViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(UiState(request = savedState.getOrThrow(ARG_REQUEST)))
    val uiState = _uiState.asStateFlow()

    init {
        sendTransactionRequest()
    }

    private fun sendTransactionRequest() {
        _uiState.update { it.copy(loading = true, error = null) }
        launchOnIo {
            exolixRepository.createExchange(_uiState.value.request)
                .onSuccess { response ->
                    val qrBitmap = QrUtil.getQrEncodedBitmapOrNull(
                        content = response.depositAddress,
                        size = resourceManager.getDimenInPx(R.dimen.wallet_info_img_qr_code_size),
                    )
                    _uiState.update {
                        it.copy(
                            exchangeId = response.id,
                            depositAddress = response.depositAddress,
                            qrBitmap = qrBitmap,
                            loading = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = exception.message ?: resourceManager.getString(R.string.exchange_create_exchange_error)
                        )
                    }
                }
        }
    }

    fun onRetry() {
        sendTransactionRequest()
    }

    fun onCopyAmount() {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.exchange_amount_label),
            clipText = _uiState.value.request.amount.toString(),
            toastMessage = resourceManager.getString(R.string.exchange_amount_copied),
        )
    }

    fun onCopyAddress() {
        _uiState.value.depositAddress?.let { address ->
            copyToClipboard(
                clipLabel = resourceManager.getString(R.string.exchange_address_label),
                clipText = address,
                toastMessage = resourceManager.getString(R.string.exchange_address_copied),
            )
        }
    }

    data class UiState(
        val request: ExchangeRequestData,
        val exchangeId: String? = null,
        val depositAddress: String? = null,
        val qrBitmap: Bitmap? = null,
        val loading: Boolean = true,
        val error: String? = null,
    )
}
