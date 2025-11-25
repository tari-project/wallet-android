package com.tari.android.wallet.ui.screen.exchange.status

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.exchange.status.ExchangeStatusFragment.Companion.ARG_TRANSACTION_ID
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.isTrue
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val TX_STATUS_AUTO_REFRESH_INTERVAL_MS = 10_000L

class ExchangeStatusViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val transactionId: String = savedState.getOrThrow(ARG_TRANSACTION_ID)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    init {
        loadTransaction()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }

    fun loadTransaction(isRefreshing: Boolean = false) {
        if (_uiState.value.transaction != null && isRefreshing) {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
        } else {
            _uiState.update { it.copy(loading = true, error = null) }
        }

        launchOnIo {
            exolixRepository.getTransaction(transactionId)
                .onSuccess { transaction ->
                    _uiState.update {
                        it.copy(
                            transaction = transaction,
                            loading = false,
                            isRefreshing = false,
                        )
                    }
                    if (!transaction.status.isFinal()) {
                        startAutoRefresh()
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            isRefreshing = false,
                            error = resourceManager.getString(R.string.exchange_status_load_error),
                        )
                    }
                }
        }
    }

    fun onRefreshClicked() {
        stopAutoRefresh()
        loadTransaction(isRefreshing = true)
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        _uiState.update { it.copy(autoRefreshActive = true) }
        autoRefreshJob = launchOnIo {
            delay(TX_STATUS_AUTO_REFRESH_INTERVAL_MS)
            loadTransaction(isRefreshing = true)
        }
    }

    private fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
        _uiState.update { it.copy(autoRefreshActive = false) }
    }

    fun onShowQrCodeClicked() {
        _uiState.value.transaction?.let { transaction ->
            launchOnMain { tariNavigator.navigate(Navigation.Exchange.SendFunds(transaction = transaction)) }
        }
    }

    fun onCopyClicked(value: String) {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.exchange_status_transaction_details),
            clipText = value,
        )
    }

    data class UiState(
        val transaction: Exolix.Transaction? = null,
        val loading: Boolean = false,
        val isRefreshing: Boolean = false,
        val error: String? = null,
        val autoRefreshActive: Boolean = false,
    ) {
        val showQrCodeButton: Boolean
            get() = transaction?.waitingForDeposit.isTrue()
    }
}
