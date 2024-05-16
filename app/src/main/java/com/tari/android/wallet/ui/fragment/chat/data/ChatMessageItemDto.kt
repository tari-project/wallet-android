package com.tari.android.wallet.ui.fragment.chat.data

data class ChatMessageItemDto(
    val uuid: String,
    val message: String,
    val date: String,
    val isMine: Boolean,
    val isRead: Boolean,
)