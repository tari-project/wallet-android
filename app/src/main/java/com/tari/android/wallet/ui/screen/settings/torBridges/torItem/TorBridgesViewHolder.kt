package com.tari.android.wallet.ui.screen.settings.torBridges.torItem

import com.tari.android.wallet.databinding.ItemBridgeBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.setVisible

class TorBridgesViewHolder(view: ItemBridgeBinding) : CommonViewHolder<TorBridgeViewHolderItem, ItemBridgeBinding>(view) {

    override fun bind(item: TorBridgeViewHolderItem) {
        super.bind(item)

        ui.tvName.text = item.title

        val isNext = item is TorBridgeViewHolderItem.CustomBridges
        ui.done.setVisible(item.isSelected && !isNext)
        ui.next.setVisible(isNext)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemBridgeBinding::inflate, TorBridgeViewHolderItem::class.java) { TorBridgesViewHolder(it as ItemBridgeBinding) }
    }
}