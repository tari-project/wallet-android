package com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact

import com.tari.android.wallet.databinding.ItemContactlessPaymentBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ContactlessPaymentItemViewHolder(view: ItemContactlessPaymentBinding) : CommonViewHolder<ContactlessPaymentItem, ItemContactlessPaymentBinding>(view) {

    override fun bind(item: ContactlessPaymentItem) {
        super.bind(item)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemContactlessPaymentBinding::inflate, ContactlessPaymentItem::class.java) { ContactlessPaymentItemViewHolder(it as ItemContactlessPaymentBinding) }
    }
}