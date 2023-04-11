package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.contacts.BadgeViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

class ContactItem(
    val contact: ContactDto,
    val isSimple: Boolean = false,
    var isSelectionState: Boolean = false,
    var isSelected: Boolean = false,
    val contactAction: (ContactDto, ContactAction) -> Unit = { _, _ -> },
    val badgeViewModel: BadgeViewModel = BadgeViewModel()
) : CommonViewHolderItem() {
    fun filtered(text: String): Boolean = contact.filtered(text)
}

