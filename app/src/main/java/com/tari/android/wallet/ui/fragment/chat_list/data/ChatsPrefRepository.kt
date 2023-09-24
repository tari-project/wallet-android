package com.tari.android.wallet.ui.fragment.chat_list.data

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatsPrefRepository @Inject constructor(
    networkRepository: NetworkRepository,
    val sharedPrefs: SharedPreferences
) : CommonRepository(networkRepository) {

    private var savedChats: ChatList? by SharedPrefGsonDelegate(sharedPrefs, this, formatKey(KEY_SAVED_CHATS), ChatList::class.java, ChatList())

    fun getSavedChats(): List<ChatItemDto> = savedChats.orEmpty().map { it }

    @Synchronized
    fun saveChats(list: List<ChatItemDto>) {
        savedChats = ChatList(list.toList())
    }

    fun clear() {
        savedChats = null
    }

    companion object {
        const val KEY_SAVED_CHATS = "KEY_SAVED_CHATS"
    }
}