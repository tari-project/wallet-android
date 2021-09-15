package com.tari.android.wallet.ui.fragment.debug.baseNodeConfig.changeBaseNode.adapter

import com.tari.android.wallet.databinding.ItemBaseNodeBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.visible

class BaseNodeViewHolder(view: ItemBaseNodeBinding) : CommonViewHolder<BaseNodeViewHolderItem, ItemBaseNodeBinding>(view) {

    override fun bind(item: BaseNodeViewHolderItem) {
        super.bind(item)

        ui.tvName.text = item.baseNodeDto.name
        ui.tvHex.text = item.baseNodeDto.publicKeyHex
        ui.tvOnionAddress.text = item.baseNodeDto.address

        ui.deleteButton.gone()
        ui.done.gone()

        if (item.baseNodeDto == item.currentBaseNode) {
            ui.done.visible()
        } else if (item.baseNodeDto.isCustom) {
            ui.deleteButton.visible()
        }

        ui.deleteButton.setOnThrottledClickListener { item.deleteAction(item.baseNodeDto) }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemBaseNodeBinding::inflate, BaseNodeViewHolderItem::class.java) { BaseNodeViewHolder(it as ItemBaseNodeBinding) }
    }
}