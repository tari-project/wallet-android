package com.tari.android.wallet.ui.fragment.settings.allSettings.title

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.HashcodeUtils

class SettingsTitleViewHolderItem(val title: String) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "SettingsTitleViewHolderItem$title"

    override fun hashCode(): Int = HashcodeUtils.generate(title)
}