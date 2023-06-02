package com.tari.android.wallet.ui.fragment.settings.allSettings.about.list

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class TariIconViewHolderItem(val icon: Int, val text: Int, val iconLink: Int) : CommonViewHolderItem() {
    override val viewHolderUUID: String = icon.toString() + text.toString() + iconLink.toString()
}