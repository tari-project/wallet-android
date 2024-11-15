package com.tari.android.wallet.ui.fragment.chat.chatList.adapter

import com.tari.android.wallet.extension.formatToRelativeTime
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.chat.data.ChatItemDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.model.EmojiId

data class ChatItemViewHolderItem(
    val walletAddress: TariWalletAddress,
    val uuid: String,
    val avatar: String,
    val alias: String,
    val emojiId: EmojiId,
    val subtitle: String,
    val dateMessage: String,
    val unreadCount: Int,
    val isOnline: Boolean,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = uuid

    constructor(
        dto: ChatItemDto,
        contact: ContactDto,
        isOnline: Boolean,
        resourceManager: ResourceManager,
    ) : this(
        walletAddress = dto.walletAddress,
        uuid = dto.uuid,
        avatar = contact.getPhoneContactInfo()?.avatar.orEmpty(),
        alias = contact.contactInfo.getAlias(),
        emojiId = dto.walletAddress.fullEmojiId,
        subtitle = dto.lastMessage?.message.orEmpty(),
        dateMessage = dto.lastMessage?.date?.formatToRelativeTime(resourceManager).orEmpty(),
        unreadCount = dto.messages.count { it.isRead.not() },
        isOnline = isOnline,
    )
}