package com.tari.android.wallet.ui.common.recyclerView.viewHolders

import com.tari.android.wallet.databinding.ItemVerticalSpaceBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.extension.dpToPx

class SpaceVerticalViewHolder(view: ItemVerticalSpaceBinding) : CommonViewHolder<SpaceVerticalViewHolderItem, ItemVerticalSpaceBinding>(view) {

    override fun bind(item: SpaceVerticalViewHolderItem) {
        super.bind(item)

        val params = ui.llRoot.layoutParams ?: return
        params.height = ui.root.context.dpToPx(item.space.toFloat()).toInt()
        ui.llRoot.layoutParams = params
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(
            ItemVerticalSpaceBinding::inflate,
            SpaceVerticalViewHolderItem::class.java
        ) { view -> SpaceVerticalViewHolder(view as ItemVerticalSpaceBinding) }
    }
}

