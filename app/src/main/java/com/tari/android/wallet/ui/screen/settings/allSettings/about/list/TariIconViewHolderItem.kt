package com.tari.android.wallet.ui.screen.settings.allSettings.about.list

import androidx.annotation.StringRes
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class TariIconViewHolderItem(val icon: Int, val text: Int, @StringRes val iconLink: Int) : CommonViewHolderItem() {
    override val viewHolderUUID: String = icon.toString() + text.toString() + iconLink.toString()
}