package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.TitleViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactItemViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.ContactlessPaymentItemViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.emptyState.EmptyStateViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolder

class ContactListAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(
        TitleViewHolder.getBuilder(),
        ContactItemViewHolder.getBuilder(),
        SettingsTitleViewHolder.getBuilder(),
        EmptyStateViewHolder.getBuilder(),
        ContactlessPaymentItemViewHolder.getBuilder()
    )
}