package com.tari.android.wallet.ui.screen.exchange.status

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.exchange.status.ExchangeStatusFragment.Companion.ARG_TRANSACTION_ID
import com.tari.android.wallet.util.extension.getOrThrow
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ExchangeStatusViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val transactionId: String = savedState.getOrThrow(ARG_TRANSACTION_ID)

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTransaction()
    }

    fun loadTransaction() {
        _uiState.update { it.copy(loading = true, error = null) }
        launchOnIo {
            exolixRepository.getTransaction(transactionId)
                .onSuccess { transaction ->
                    _uiState.update {
                        it.copy(
                            transaction = transaction,
                            loading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = resourceManager.getString(R.string.exchange_status_load_error, WalletError(e).code),
                        )
                    }
                }
        }
    }

    fun onTransactionDetailsClicked() {
        showNotReadyYetDialog()
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
        val error: String? = null,
    )
}
