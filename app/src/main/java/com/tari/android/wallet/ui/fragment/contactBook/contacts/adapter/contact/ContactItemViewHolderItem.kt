package com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.HashcodeUtils

data class ContactItemViewHolderItem(
    val contact: ContactDto,
    val isSimple: Boolean = false,
    var isSelectionState: Boolean = false,
    var isSelected: Boolean = false,
) : CommonViewHolderItem() {
    fun filtered(text: String): Boolean = contact.filtered(text)

    override val viewHolderUUID
        get() = contact.uuid

    override fun hashCode(): Int = HashcodeUtils.generate(contact, isSimple, isSelectionState, isSelected, contact.contactInfo.isFavorite)

    override fun equals(other: Any?): Boolean {
        if (other is ContactItemViewHolderItem) {
            return contact == other.contact &&
                    isSimple == other.isSimple &&
                    isSelectionState == other.isSelectionState &&
                    isSelected == other.isSelected
        }
        return false
    }
}

