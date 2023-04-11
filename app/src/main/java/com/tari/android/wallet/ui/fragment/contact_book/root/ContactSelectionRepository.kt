package com.tari.android.wallet.ui.fragment.contact_book.root

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactSelectionRepository @Inject constructor() : CommonViewModel() {
    val selectedContacts = mutableListOf<ContactItem>()

    val isSelectionState = MutableLiveData(false)

    val isPossibleToShare = MutableLiveData(false)

    fun toggle(item: ContactItem) {
        if (item.contact.getFFIDto() == null) return

        if (selectedContacts.contains(item)) {
            selectedContacts.remove(item)
            item.isSelected = false
        } else {
            selectedContacts.add(item)
            item.isSelected = true
        }
        isPossibleToShare.postValue(selectedContacts.isNotEmpty())
        item.rebind()
    }

    fun clear() {
        isSelectionState.postValue(false)
        selectedContacts.forEach { it.isSelected = false }
        selectedContacts.clear()
    }
}