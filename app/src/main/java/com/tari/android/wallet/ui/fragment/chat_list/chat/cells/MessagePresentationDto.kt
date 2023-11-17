package com.tari.android.wallet.ui.fragment.chat_list.chat.cells

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

data class MessagePresentationDto(val message: String, val isMine: Boolean) : CommonViewHolderItem() {
    override val viewHolderUUID: String = "MessageCellViewHolder + $message + $isMine"
}