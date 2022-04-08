package com.tari.android.wallet.ui.fragment.settings.allSettings.divider

import com.tari.android.wallet.databinding.ItemSettingsDividerBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem

class SettingsDividerViewHolder(view: ItemSettingsDividerBinding) : CommonViewHolder<DividerViewHolderItem, ItemSettingsDividerBinding>(view) {

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemSettingsDividerBinding::inflate, DividerViewHolderItem::class.java) {
            SettingsDividerViewHolder(it as ItemSettingsDividerBinding)
        }
    }
}