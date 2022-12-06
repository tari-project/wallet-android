package com.tari.android.wallet.ui.fragment.settings.themeSelector.adapter

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemThemeBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme

class ThemesViewHolder(view: ItemThemeBinding) : CommonViewHolder<ThemeViewHolderItem, ItemThemeBinding>(view) {

    override fun bind(item: ThemeViewHolderItem) {
        super.bind(item)

        val text = when (item.theme) {
            TariTheme.AppBased -> R.string.select_theme_app_based
            TariTheme.Light -> R.string.select_theme_light
            TariTheme.Dark -> R.string.select_theme_dark
            TariTheme.Purple -> R.string.select_theme_purple
        }
        ui.title.text = itemView.context.getString(text)
        ui.selectedImage.setVisible(item.theme == item.currentTheme)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemThemeBinding::inflate, ThemeViewHolderItem::class.java) { ThemesViewHolder(it as ItemThemeBinding) }
    }
}