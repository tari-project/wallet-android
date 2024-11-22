package com.tari.android.wallet.ui.screen.chat.chatDetails.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.data.chat.ChatMessageItemDto

data class ChatMessageViewHolderItem(
    val message: String,
    val isMine: Boolean,
) : CommonViewHolderItem() {
    constructor(dto: ChatMessageItemDto) : this(dto.message, dto.isMine)

    override val viewHolderUUID: String = "MessageCellViewHolder + $message + $isMine"
}