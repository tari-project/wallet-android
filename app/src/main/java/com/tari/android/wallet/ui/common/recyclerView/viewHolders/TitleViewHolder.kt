package com.tari.android.wallet.ui.common.recyclerView.viewHolders

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemTitleBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.util.extension.dimen
import com.tari.android.wallet.util.extension.setTopMargin

class TitleViewHolder(view: ItemTitleBinding) : CommonViewHolder<TitleViewHolderItem, ItemTitleBinding>(view) {

    override fun bind(item: TitleViewHolderItem) {
        super.bind(item)

        ui.tvTitle.text = item.title

        val topMargin = if (item.isFirst) R.dimen.common_zero else R.dimen.home_header_top_margin
        ui.tvTitle.setTopMargin(dimen(topMargin))
    }

    companion object {
        fun getBuilder() =
            ViewHolderBuilder(ItemTitleBinding::inflate, TitleViewHolderItem::class.java) { view -> TitleViewHolder(view as ItemTitleBinding) }
    }
}