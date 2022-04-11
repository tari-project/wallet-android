package com.tari.android.wallet.ui.fragment.settings.allSettings.button

import com.tari.android.wallet.databinding.ItemButtonBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ButtonViewHolder(view: ItemButtonBinding) : CommonViewHolder<ButtonViewDto, ItemButtonBinding>(view) {

    override fun bind(item: ButtonViewDto) {
        super.bind(item)
        ui.button.initDto(item)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemButtonBinding::inflate, ButtonViewDto::class.java) {
            ButtonViewHolder(it as ItemButtonBinding)
        }
    }
}