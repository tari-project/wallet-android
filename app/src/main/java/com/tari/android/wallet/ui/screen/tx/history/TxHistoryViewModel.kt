package com.tari.android.wallet.ui.screen.tx.history

import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.collectFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TxHistoryViewModel() : CommonViewModel() {

    @Inject
    lateinit var txRepository: TxRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            pendingTxs = emptyList(),
            nonPendingTxs = emptyList(),
            ticker = networkRepository.currentNetwork.ticker,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(txRepository.txs) { txs ->
            _uiState.update {
                it.copy(
                    pendingTxs = txs.pendingTxs,
                    nonPendingTxs = txs.nonPendingTxs,
                )
            }
        }
    }

    fun navigateToTxDetail(tx: Tx) {
        tariNavigator.navigate(Navigation.TxList.ToTxDetails(tx))
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    data class UiState(
        val pendingTxs: List<TxDto>,
        val nonPendingTxs: List<TxDto>,
        val ticker: String,
        val searchQuery: String = "",
    ) {
        val allTxList: List<TxDto>
            get() = pendingTxs + nonPendingTxs

        val sortedTxList: List<TxDto>
            get() = allTxList.filterByQuery()

        val showEmptyState: Boolean
            get() = allTxList.isEmpty()
        val showSortedList: Boolean
            get() = allTxList.isNotEmpty() && searchQuery.isNotBlank()

        private fun List<TxDto>.filterByQuery(): List<TxDto> = searchQuery.trim().takeIf { it.isNotBlank() }
            ?.let { query -> filter { it.contains(query) } } ?: this
    }
}