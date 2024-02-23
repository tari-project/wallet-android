package com.tari.android.wallet.ui.fragment.settings.allSettings.row

import com.tari.android.wallet.databinding.ItemSettingsRowBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class SettingsRowViewHolder(view: ItemSettingsRowBinding) : CommonViewHolder<SettingsRowViewDto, ItemSettingsRowBinding>(view) {

    override fun bind(item: SettingsRowViewDto) {
        super.bind(item)
        ui.button.initDto(item)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemSettingsRowBinding::inflate, SettingsRowViewDto::class.java) {
            SettingsRowViewHolder(it as ItemSettingsRowBinding)
        }
    }
}