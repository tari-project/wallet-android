package com.tari.android.wallet.ui.fragment.contactBook.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.ContactItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSelectionRepository @Inject constructor() {
    val selectedContacts = mutableListOf<ContactItem>()

    val isSelectionState = MutableLiveData(false)

    val isPossibleToShare = MutableLiveData(false)

    fun toggle(item: ContactItem) {
        if (item.contact.getFFIContactInfo() == null) return

        val contact = selectedContacts.firstOrNull { it.contact.uuid == item.contact.uuid }
        if (contact != null) {
            selectedContacts.remove(contact)
        } else {
            selectedContacts.add(item)
        }
        isPossibleToShare.postValue(selectedContacts.isNotEmpty())
    }

    fun clear() {
        selectedContacts.clear()
        isSelectionState.postValue(false)
    }
}