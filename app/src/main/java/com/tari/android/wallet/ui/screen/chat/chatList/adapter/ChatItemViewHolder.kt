package com.tari.android.wallet.ui.screen.chat.chatList.adapter

import com.tari.android.wallet.databinding.ItemChatItemBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.visible

class ChatItemViewHolder(view: ItemChatItemBinding) : CommonViewHolder<ChatItemViewHolderItem, ItemChatItemBinding>(view) {

    override fun bind(item: ChatItemViewHolderItem) {
        super.bind(item)

        with(ui) {
            unreadCountContainer.setVisible(item.unreadCount > 0)
            unreadCount.text = item.unreadCount.toString()
            date.text = item.dateMessage
            message.text = item.subtitle
            alias.text = item.alias

            if (item.alias.isEmpty()) {
                emojiIdViewContainer.root.visible()
                alias.gone()
            } else {
                emojiIdViewContainer.root.gone()
                alias.text = item.alias
                alias.visible()
            }
        }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemChatItemBinding::inflate, ChatItemViewHolderItem::class.java) { ChatItemViewHolder(it as ItemChatItemBinding) }
    }
}