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
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TX_STATUS_AUTO_REFRESH_INTERVAL_MS = 10_000L

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

    private var autoRefreshJob: Job? = null

    init {
        _uiState.value.transaction?.let {
            initializeFromTransaction(it)
        } ?: run {
            loadTransaction()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
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

    private fun refreshTransactionStatus() {
        val transactionId = _uiState.value.transaction?.id ?: return

        launchOnIo {
            val transaction = exolixRepository.getTransaction(transactionId).getOrNull()

            if (transaction != null && transaction.status != Exolix.TransactionStatus.WAIT) {
                stopAutoRefresh()
                openTxDetails()
            } else {
                startAutoRefresh()
            }
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = launchOnIo {
            delay(TX_STATUS_AUTO_REFRESH_INTERVAL_MS)
            refreshTransactionStatus()
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
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
        startAutoRefresh()
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

    fun openTxDetails() {
        _uiState.value.transaction?.let { transaction ->
            launchOnMain {
                tariNavigator.navigateSequence(
                    Navigation.BackToHome,
                    Navigation.Exchange.ExchangeStatus(transaction.id),
                )
            }
        }
    }

    data class UiState(
        val transaction: Exolix.Transaction? = null,

        val qrBitmap: Bitmap? = null,

        val loading: Boolean = false,
        val error: String? = null,
    )
}
