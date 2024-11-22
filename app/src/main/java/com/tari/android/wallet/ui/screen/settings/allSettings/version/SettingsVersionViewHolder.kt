package com.tari.android.wallet.ui.screen.settings.allSettings.version

import com.tari.android.wallet.databinding.ItemSettingsVersionBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SettingsVersionViewHolder(view: ItemSettingsVersionBinding) :
    CommonViewHolder<SettingsVersionViewHolderItem, ItemSettingsVersionBinding>(view) {

    override fun bind(item: SettingsVersionViewHolderItem) {
        super.bind(item)
        ui.networkInfoTextView.text = item.version
        ui.root.setOnClickListener { item.action.invoke() }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemSettingsVersionBinding::inflate, SettingsVersionViewHolderItem::class.java) {
            SettingsVersionViewHolder(it as ItemSettingsVersionBinding)
        }
    }
}