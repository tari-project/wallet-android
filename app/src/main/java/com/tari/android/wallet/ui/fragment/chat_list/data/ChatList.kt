package com.tari.android.wallet.ui.fragment.chat_list.data

import java.io.Serializable

class ChatList() : ArrayList<ChatItemDto>(), Serializable {
    constructor(list: List<ChatItemDto>) : this() {
        this.addAll(list)
    }
}

fun ChatList?.orEmpty(): ChatList = this ?: ChatList()