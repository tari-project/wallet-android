package com.tari.android.wallet.ui.screen.contactBook.obsolete.details.adapter.contactType

import com.tari.android.wallet.databinding.ItemContactTypeBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ContactTypeViewHolder(view: ItemContactTypeBinding) : CommonViewHolder<ContactTypeViewHolderItem, ItemContactTypeBinding>(view) {

    override fun bind(item: ContactTypeViewHolderItem) {
        super.bind(item)

        ui.contactIconType.setImageResource(item.icon)
        ui.contactTypeIconText.text = item.type
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemContactTypeBinding::inflate, ContactTypeViewHolderItem::class.java) {
            ContactTypeViewHolder(it as ItemContactTypeBinding)
        }
    }
}

