package com.tari.android.wallet.data.sharedPrefs.chat

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.data.chat.ChatItemDto
import com.tari.android.wallet.data.chat.ChatMessageItemDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatsPrefRepository @Inject constructor(
    val sharedPrefs: SharedPreferences,
    networkRepository: NetworkPrefRepository,
) : CommonPrefRepository(networkRepository) {

    private var savedChats: ChatList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(KEY_SAVED_CHATS),
        type = ChatList::class.java,
        defValue = ChatList(),
    )

    fun getSavedChats(): List<ChatItemDto> = savedChats.map { it }

    @Synchronized
    fun saveChats(list: List<ChatItemDto>) {
        savedChats = ChatList(list.toList())
    }

    @Synchronized
    fun addChat(chatItemDto: ChatItemDto) {
        val list = savedChats
        list.add(chatItemDto)
        saveChats(list.toList())
    }

    fun saveMessage(chatItemDto: ChatItemDto?, messageItemDto: ChatMessageItemDto) {
        val list = savedChats
        val index = list.indexOfFirst { it.uuid == chatItemDto?.uuid }
        if (index != -1) {
            val chatItem = list[index]
            val messages = chatItem.messages.toMutableList()
            messages.add(messageItemDto)
            list[index] = ChatItemDto(chatItem.uuid, messages.toList(), chatItem.walletAddress)
            saveChats(list.toList())
        }
    }

    fun clear() {
        savedChats = ChatList()
    }

    companion object {
        const val KEY_SAVED_CHATS = "KEY_SAVED_CHATS"
    }
}

class ChatList(list: List<ChatItemDto>) : ArrayList<ChatItemDto>(list) {
    constructor() : this(emptyList())
}