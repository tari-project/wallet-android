package com.tari.android.wallet.ui.fragment.chat_list.chat.cells

import com.tari.android.wallet.databinding.ItemChatItemBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class MessageCellViewHolder(ui: ItemChatItemBinding) : CommonViewHolder<MessagePresentationDto, ItemChatItemBinding>(ui) {

    override fun bind(item: MessagePresentationDto) {
        super.bind(item)

        //todo
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemChatItemBinding::inflate, MessagePresentationDto::class.java) {
            MessageCellViewHolder(it as ItemChatItemBinding)
        }
    }
}