package com.tari.android.wallet.ui.fragment.chat_list.adapter

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

data class ChatItemViewHolderItem(
    val walletAddress: TariWalletAddress,
    val uuid: String,
    val firstEmoji: String,
    val avatar: String,
    val alias: String,
    val emojiId: String,
    val subtitle: String,
    val dateMessage: String,
    val unreadCount: Int,
    val isOnline: Boolean,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = ""
}