package com.tari.android.wallet.ui.screen.settings.allSettings.title

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.util.HashcodeUtils

class SettingsTitleViewHolderItem(val title: String) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "SettingsTitleViewHolderItem$title"

    override fun hashCode(): Int = HashcodeUtils.generate(title)
}