package com.tari.android.wallet.ui.screen.chat.chatList

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

object ChatListModel {

    data class UiState(
        val chatList: List<CommonViewHolderItem> = emptyList()
    ){
        val showEmpty: Boolean
            get() = chatList.isEmpty()
    }
}