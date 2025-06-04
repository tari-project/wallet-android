package com.tari.android.wallet.ui.screen.tx.history

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.zipToPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class TxHistoryViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var txRepository: TxRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            selectedContact = savedState.get<Contact>(TariNavigator.PARAMETER_CONTACT),
            pendingTxs = emptyList(),
            nonPendingTxs = emptyList(),
            ticker = networkRepository.currentNetwork.ticker,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(txRepository.pendingTxs.zipToPair(txRepository.nonPendingTxs)) { (pendingTxs, nonPendingTxs) ->
            _uiState.update {
                it.copy(
                    pendingTxs = pendingTxs,
                    nonPendingTxs = nonPendingTxs,
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
        val selectedContact: Contact? = null, // null if not contact tx history
        val pendingTxs: List<TxDto>,
        val nonPendingTxs: List<TxDto>,
        val ticker: String,
        val searchQuery: String = "",
    ) {
        val allTxList: List<TxDto>
            get() = pendingTxs + nonPendingTxs

        val sortedTxList: List<TxDto>
            get() = allTxList.filterByContact().filterByQuery()

        val showEmptyState: Boolean
            get() = allTxList.isEmpty()
        val showSortedList: Boolean
            get() = selectedContact != null || allTxList.isNotEmpty() && searchQuery.isNotBlank()

        private fun List<TxDto>.filterByContact(): List<TxDto> = selectedContact?.let { contact ->
            filter { it.tx.tariContact.walletAddress == contact.walletAddress }
        } ?: this

        private fun List<TxDto>.filterByQuery(): List<TxDto> = searchQuery.trim().takeIf { it.isNotBlank() }?.let { query ->
            filter { it.contains(query) }
        } ?: this
    }
}