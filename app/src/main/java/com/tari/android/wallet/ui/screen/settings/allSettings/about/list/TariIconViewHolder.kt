package com.tari.android.wallet.ui.screen.settings.allSettings.about.list

import androidx.core.content.ContextCompat
import com.tari.android.wallet.databinding.ItemTariIconBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class TariIconViewHolder(view: ItemTariIconBinding): CommonViewHolder<TariIconViewHolderItem, ItemTariIconBinding>(view) {
    override fun bind(item: TariIconViewHolderItem) {
        super.bind(item)

        ui.icon.setImageDrawable(ContextCompat.getDrawable(itemView.context, item.icon))
        ui.text.text = itemView.context.getText(item.text)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemTariIconBinding::inflate, TariIconViewHolderItem::class.java) {
            TariIconViewHolder(it as ItemTariIconBinding)
        }
    }
}