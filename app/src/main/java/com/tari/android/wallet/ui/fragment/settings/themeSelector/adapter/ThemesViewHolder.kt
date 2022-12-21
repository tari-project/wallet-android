package com.tari.android.wallet.ui.fragment.settings.themeSelector.adapter

import androidx.core.content.ContextCompat
import com.tari.android.wallet.databinding.ItemThemeBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ThemesViewHolder(view: ItemThemeBinding) : CommonViewHolder<ThemeViewHolderItem, ItemThemeBinding>(view) {

    override fun bind(item: ThemeViewHolderItem) {
        super.bind(item)

        ui.title.text = itemView.context.getString(item.theme.text)
        ui.image.setImageDrawable(ContextCompat.getDrawable(itemView.context, item.theme.image))
        ui.checkbox.apply {
            setOnCheckedChangeListener(null)
            isChecked = item.theme == item.currentTheme
            if (!isChecked) {
                setOnCheckedChangeListener { _, isChecked ->
                    item.checkboxSelection(isChecked)
                }
            }
            ui.checkbox.isClickable = !isChecked
        }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemThemeBinding::inflate, ThemeViewHolderItem::class.java) { ThemesViewHolder(it as ItemThemeBinding) }
    }
}