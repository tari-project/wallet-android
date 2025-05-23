package com.tari.android.wallet.ui.screen.settings.themeSelector.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

class ThemeViewHolderItem(val theme: TariTheme, val currentTheme: TariTheme, val checkboxSelection: (isChecked: Boolean) -> Unit) :
    CommonViewHolderItem() {
        override val viewHolderUUID: String = "ThemeViewHolderItem$theme"
    }