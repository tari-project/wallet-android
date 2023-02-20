package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter

import com.tari.android.wallet.ui.common.recyclerView.*
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.TitleViewHolder

class ContactListAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(TitleViewHolder.getBuilder(), ContactViewHolder.getBuilder())
}