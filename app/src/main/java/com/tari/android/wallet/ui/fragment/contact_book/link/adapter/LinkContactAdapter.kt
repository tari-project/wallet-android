package com.tari.android.wallet.ui.fragment.contact_book.link.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItemViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.link.adapter.link_header.ContactLinkHeaderViewHolder

class LinkContactAdapter : CommonAdapter<CommonViewHolderItem>() {

    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(
        ContactItemViewHolder.getBuilder(),
        ContactLinkHeaderViewHolder.getBuilder()
    )
}