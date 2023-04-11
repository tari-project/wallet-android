package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSelectionRepository @Inject constructor(): CommonViewModel() {
    val selectedContacts = mutableListOf<ContactItem>()

    val isSelectionState = MutableLiveData(false)

    fun toggle(item: ContactItem) {
        if (selectedContacts.contains(item)) {
            selectedContacts.remove(item)
        } else {
            selectedContacts.add(item)
        }
    }
}