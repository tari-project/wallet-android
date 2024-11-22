package com.tari.android.wallet.ui.screen.settings.allSettings.myProfile

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

data class MyProfileViewHolderItem(
    val address: TariWalletAddress,
    val yat: String,
    val alias: String,
    val action: () -> Unit,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "MyProfileViewHolderItem${address.fullEmojiId}"
}