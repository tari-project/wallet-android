package com.tari.android.wallet.ui.common.recyclerView.viewHolders

import com.tari.android.wallet.databinding.ItemDividerBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem

class DividerViewHolder(view: ItemDividerBinding) : CommonViewHolder<DividerViewHolderItem, ItemDividerBinding>(view) {
    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemDividerBinding::inflate, DividerViewHolderItem::class.java) { DividerViewHolder(it as ItemDividerBinding) }
    }
}