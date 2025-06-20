package com.tari.android.wallet.ui.screen.contactBook.obsolete.contacts.adapter.emptyState

import com.tari.android.wallet.databinding.ItemContactEmptyStateBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.setVisible

class EmptyStateViewHolder(view: ItemContactEmptyStateBinding) : CommonViewHolder<EmptyStateViewHolderItem, ItemContactEmptyStateBinding>(view) {

    override fun bind(item: EmptyStateViewHolderItem) {
        super.bind(item)

        ui.emptyStateTitleView.text = item.title
        ui.emptyStateBodyView.text = item.body
        ui.emptyStateImageView.setImageResource(item.image)
        ui.emptyStateButton.setOnClickListener { item.action.invoke() }
        ui.emptyStateButton.ui.button.text = item.buttonTitle
        ui.emptyStateButton.setOnClickListener { item.action.invoke() }
        ui.emptyStateButton.setVisible(item.buttonTitle.isNotEmpty())
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(
                ItemContactEmptyStateBinding::inflate,
                EmptyStateViewHolderItem::class.java
            ) { EmptyStateViewHolder(it as ItemContactEmptyStateBinding) }
    }
}