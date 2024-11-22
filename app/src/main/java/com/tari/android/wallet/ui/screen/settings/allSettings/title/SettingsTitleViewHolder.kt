package com.tari.android.wallet.ui.screen.settings.allSettings.title

import com.tari.android.wallet.databinding.ItemSettingsTitleBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SettingsTitleViewHolder(view: ItemSettingsTitleBinding) : CommonViewHolder<SettingsTitleViewHolderItem, ItemSettingsTitleBinding>(view) {

    override fun bind(item: SettingsTitleViewHolderItem) {
        super.bind(item)
        ui.title.initDto(item)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemSettingsTitleBinding::inflate, SettingsTitleViewHolderItem::class.java) {
            SettingsTitleViewHolder(it as ItemSettingsTitleBinding)
        }
    }
}