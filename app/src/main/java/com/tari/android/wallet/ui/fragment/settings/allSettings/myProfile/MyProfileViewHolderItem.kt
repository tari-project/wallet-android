package com.tari.android.wallet.ui.fragment.settings.allSettings.myProfile

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class MyProfileViewHolderItem(val emojiId: String, val yat: String, val alias: String, val action: () -> Unit) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "MyProfileViewHolderItem$emojiId"
}