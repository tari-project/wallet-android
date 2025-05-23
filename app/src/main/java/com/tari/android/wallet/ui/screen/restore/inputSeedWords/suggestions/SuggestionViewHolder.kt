package com.tari.android.wallet.ui.screen.restore.inputSeedWords.suggestions

import com.tari.android.wallet.databinding.ItemSuggestionBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SuggestionViewHolder(binding: ItemSuggestionBinding): CommonViewHolder<SuggestionViewHolderItem, ItemSuggestionBinding>(binding) {

    override fun bind(item: SuggestionViewHolderItem) {
        super.bind(item)

        ui.tvTitle.text = item.suggestion
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(
                ItemSuggestionBinding::inflate,
                SuggestionViewHolderItem::class.java
            ) { SuggestionViewHolder(it as ItemSuggestionBinding) }
    }
}