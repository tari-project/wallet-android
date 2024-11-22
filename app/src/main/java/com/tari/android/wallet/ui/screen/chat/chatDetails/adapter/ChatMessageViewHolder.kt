package com.tari.android.wallet.ui.screen.chat.chatDetails.adapter

import com.tari.android.wallet.databinding.ItemChatMessageItemBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class ChatMessageViewHolder(ui: ItemChatMessageItemBinding) : CommonViewHolder<ChatMessageViewHolderItem, ItemChatMessageItemBinding>(ui) {

    override fun bind(item: ChatMessageViewHolderItem) {
        super.bind(item)

        ui.message.text = item.message
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemChatMessageItemBinding::inflate, ChatMessageViewHolderItem::class.java) {
            ChatMessageViewHolder(it as ItemChatMessageItemBinding)
        }
    }
}