package com.tari.android.wallet.ui.screen.home.allSwaps

import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.data.exolix.ExolixRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AllSwapsViewModel : CommonViewModel() {

    @Inject
    lateinit var exolixRepository: ExolixRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSwaps()
    }

    fun loadSwaps() {
        _uiState.update { it.copy(loadingState = TariLoadingLayoutState.Loading) }

        launchOnIo {
            exolixRepository.getPendingTransactions()
                .onSuccess { transactions ->
                    _uiState.update {
                        it.copy(
                            swaps = transactions,
                            loadingState = TariLoadingLayoutState.Content,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(loadingState = TariLoadingLayoutState.Error) }
                }
        }
    }

    fun onSwapClick(transaction: Exolix.Transaction) {
        if (transaction.waitingForDeposit) {
            tariNavigator.navigate(Navigation.Exchange.SendFunds(transaction = transaction))
        } else {
            tariNavigator.navigate(Navigation.Exchange.ExchangeStatus(transaction.id))
        }
    }

    data class UiState(
        val swaps: List<Exolix.Transaction> = emptyList(),
        val loadingState: TariLoadingLayoutState = TariLoadingLayoutState.Loading,
    )
}
