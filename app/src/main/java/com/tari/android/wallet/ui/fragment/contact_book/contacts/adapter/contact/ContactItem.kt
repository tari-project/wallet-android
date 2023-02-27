package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto

class ContactItem(val contact: ContactDto, val position: Int) : CommonViewHolderItem() {
    fun filtered(text: String) : Boolean = contact.filtered(text)
}

