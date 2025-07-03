package com.tari.android.wallet.ui.screen.contactBook.obsolete.contacts.adapter.contact

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.util.HashcodeUtils

data class ContactItemViewHolderItem(
    val contact: Contact,
    val isSimple: Boolean = false,
    var isSelectionState: Boolean = false,
    var isSelected: Boolean = false,
) : CommonViewHolderItem() {
    override val viewHolderUUID
        get() = contact.walletAddress.fullBase58

    override fun hashCode(): Int = HashcodeUtils.generate(contact, isSimple, isSelectionState, isSelected)

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

