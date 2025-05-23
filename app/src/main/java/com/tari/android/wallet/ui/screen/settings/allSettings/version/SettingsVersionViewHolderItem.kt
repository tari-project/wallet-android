package com.tari.android.wallet.ui.screen.settings.allSettings.version

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class SettingsVersionViewHolderItem(val version: String, val action: () -> Unit) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "SettingsVersionViewHolder$version"
}