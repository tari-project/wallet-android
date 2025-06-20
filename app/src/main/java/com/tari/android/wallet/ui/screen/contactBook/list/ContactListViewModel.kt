package com.tari.android.wallet.ui.screen.contactBook.list

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class ContactListViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    fun onAddContactClicked() {
        showNotReadyYetDialog()
    }

    data class UiState(
        val contacts: List<Contact> = emptyList(),
    )
}