package com.tari.android.wallet.ui.fragment.settings.allSettings.title

import com.tari.android.wallet.databinding.ItemSettingsTitleBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SettingsTitleViewHolder(view: ItemSettingsTitleBinding) : CommonViewHolder<SettingsTitleDto, ItemSettingsTitleBinding>(view) {

    override fun bind(item: SettingsTitleDto) {
        super.bind(item)
        ui.title.initDto(item)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemSettingsTitleBinding::inflate, SettingsTitleDto::class.java) { SettingsTitleViewHolder(it as ItemSettingsTitleBinding) }
    }
}