package com.tari.android.wallet.ui.fragment.chat_list.adapter

import com.bumptech.glide.Glide
import com.tari.android.wallet.databinding.ItemChatItemBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible

class ChatItemViewHolder(view: ItemChatItemBinding) : CommonViewHolder<ChatItemViewHolderItem, ItemChatItemBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    override fun bind(item: ChatItemViewHolderItem) {
        super.bind(item)

        with(ui) {
            onlineStatus.setVisible(item.isOnline)
            unreadCountContainer.setVisible(item.unreadCount > 0)
            unreadCount.text = item.unreadCount.toString()
            date.text = item.dateMessage
            message.text = item.subtitle
            alias.text = item.alias

            if (item.avatar.isNotEmpty()) {
                firstEmojiTextView.gone()
                avatar.visible()
                Glide.with(avatar).load(item.avatar).into(avatar)
            } else {
                firstEmojiTextView.visible()
                avatar.gone()
                firstEmojiTextView.text = item.firstEmoji
            }

            if (item.alias.isEmpty()) {
                participantEmojiIdView.root.visible()
                alias.gone()
                emojiIdSummaryController.display(item.emojiId)
            } else {
                participantEmojiIdView.root.gone()
                alias.text = item.emojiId
                alias.visible()
            }
        }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemChatItemBinding::inflate, ChatItemViewHolderItem::class.java) { ChatItemViewHolder(it as ItemChatItemBinding) }
    }
}