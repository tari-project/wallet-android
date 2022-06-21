package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import com.tari.android.wallet.databinding.ItemUtxosTextBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class UtxosTextListViewHolder(view: ItemUtxosTextBinding) : CommonViewHolder<UtxosViewHolderItem, ItemUtxosTextBinding>(view) {

    override fun bind(item: UtxosViewHolderItem) {
        super.bind(item)

        ui.amount.text = item.amount +  " XTR"
        ui.hash.text = item.hash
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemUtxosTextBinding::inflate, UtxosViewHolderItem::class.java) { UtxosTextListViewHolder(it as ItemUtxosTextBinding) }
    }
}