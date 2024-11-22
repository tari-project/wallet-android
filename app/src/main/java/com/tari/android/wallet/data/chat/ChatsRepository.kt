package com.tari.android.wallet.data.chat

import com.tari.android.wallet.data.sharedPrefs.chat.ChatsPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatsRepository @Inject constructor(
    private val chatsPrefRepository: ChatsPrefRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val _chatList = MutableStateFlow<List<ChatItemDto>>(emptyList())
    val chatList = _chatList.asStateFlow()

    init {
        applicationScope.launch {
            chatsPrefRepository.updateNotifier.subscribe { updateList() }
        }
    }

    private fun updateList() {
        val list = if (DebugConfig.mockChatMessages) {
            MockDataStub.createChatList()
        } else {
            chatsPrefRepository.getSavedChats()
        }

        _chatList.update { list }
    }

    fun getByUuid(uuid: String): ChatItemDto? = chatList.value.find { it.uuid == uuid }

    fun getChatByWalletAddress(walletAddress: TariWalletAddress): ChatItemDto? = chatList.value.find { it.walletAddress == walletAddress }

    fun addChat(chat: ChatItemDto) {
        chatsPrefRepository.addChat(chat)
    }

    fun addMessage(walletAddress: TariWalletAddress, message: ChatMessageItemDto) {
        chatsPrefRepository.saveMessage(getChatByWalletAddress(walletAddress), message)
    }
}