package com.tari.android.wallet.ui.fragment.chat.data

import com.tari.android.wallet.model.TariWalletAddress

data class ChatItemDto(
    val uuid: String,
    val messages: List<ChatMessageItemDto>,
    val walletAddress: TariWalletAddress,
) {
    val lastMessage: ChatMessageItemDto?
        get() = messages.maxByOrNull { it.date }
}