package com.tari.android.wallet.ui.screen.contactBook.list

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.data.tx.TxListData
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.collectFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ContactListViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var txRepository: TxRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            contacts = contactsRepository.contactList.value,
            txs = txRepository.txs.value,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(contactsRepository.contactList) { contacts -> _uiState.update { it.copy(contacts = contacts) } }
        collectFlow(txRepository.txs) { txs -> _uiState.update { it.copy(txs = txs) } }
    }

    fun onAddContactClicked() {
        showNotReadyYetDialog()
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onContactItemClicked(contact: Contact) {
        tariNavigator.navigate(Navigation.ContactBook.ContactDetails(contact))
    }

    data class UiState(
        private val contacts: List<Contact>,
        private val txs: TxListData,

        val searchQuery: String = "",
    ) {
        val showEmptyState: Boolean
            get() = contacts.isEmpty()

        val sortedContactList: List<Contact>
            get() = contacts.filter { it.contains(searchQuery) }.sortedBy { it.alias }

        val recentContactList: List<Contact>
            get() = contacts
                .map { contact -> contact to txs.lastUsedTimestamp(contact) }
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
    }
}