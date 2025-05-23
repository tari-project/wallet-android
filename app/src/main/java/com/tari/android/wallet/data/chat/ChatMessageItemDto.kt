package com.tari.android.wallet.data.chat

import java.util.Date

data class ChatMessageItemDto(
    val message: String,
    val date: Date,
    val isMine: Boolean,
    val isRead: Boolean,
)