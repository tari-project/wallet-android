package com.tari.android.wallet.ui.fragment.settings.themeSelector.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.themeSelector.TariTheme

class ThemeViewHolderItem(val theme: TariTheme, val currentTheme: TariTheme, val checkboxSelection: (isChecked: Boolean) -> Unit) :
    CommonViewHolderItem() {
        override val viewHolderUUID: String = "ThemeViewHolderItem$theme"
    }