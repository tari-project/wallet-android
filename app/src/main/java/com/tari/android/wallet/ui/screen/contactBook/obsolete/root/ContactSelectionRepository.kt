package com.tari.android.wallet.ui.screen.contactBook.obsolete.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.screen.contactBook.obsolete.contacts.adapter.contact.ContactItemViewHolderItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSelectionRepository @Inject constructor() {
    val selectedContacts = mutableListOf<ContactItemViewHolderItem>()

    val isSelectionState = MutableLiveData(false)

    val isPossibleToShare = MutableLiveData(false)

    fun toggle(item: ContactItemViewHolderItem) {
        val contact = selectedContacts.firstOrNull { it.contact.walletAddress == item.contact.walletAddress }
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