package com.tari.android.wallet.ui.common.recyclerView.items

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class TitleViewHolderItem(val title: String, val isFirst: Boolean = false) : CommonViewHolderItem() {
    override val viewHolderUUID: String = title
}