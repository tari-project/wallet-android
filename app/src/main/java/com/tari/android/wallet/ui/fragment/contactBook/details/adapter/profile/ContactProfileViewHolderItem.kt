package com.tari.android.wallet.ui.fragment.contactBook.details.adapter.profile

import com.tari.android.wallet.databinding.ViewEmojiIdWithYatSummaryBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

data class ContactProfileViewHolderItem(val contactDto: ContactDto, val show: () -> Unit, val init: (ViewEmojiIdWithYatSummaryBinding) -> Unit) : CommonViewHolderItem() {
    override val viewHolderUUID: String = contactDto.uuid
}