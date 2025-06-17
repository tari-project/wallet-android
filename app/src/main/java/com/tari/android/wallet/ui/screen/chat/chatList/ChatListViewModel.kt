package com.tari.android.wallet.ui.screen.chat.chatList

import com.tari.android.wallet.data.chat.ChatItemDto
import com.tari.android.wallet.data.chat.ChatsRepository
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.chat.chatList.adapter.ChatItemViewHolderItem
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.collectFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class ChatListViewModel : CommonViewModel() {

    @Inject
    lateinit var chatsRepository: ChatsRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    private val _uiState = MutableStateFlow(ChatListModel.UiState())
    val uiState: StateFlow<ChatListModel.UiState> = _uiState.asStateFlow()

    init {
        component.inject(this)

        collectFlow(chatsRepository.chatList) { updateChatList(it) }
    }

    private fun updateChatList(chats: List<ChatItemDto>) {
        _uiState.update { state ->
            state.copy(
                chatList = chats
                    .sortedByDescending { it.lastMessage?.date }
                    .map { chat ->
                        ChatItemViewHolderItem(
                            dto = chat,
                            contact = if (DebugConfig.mockChatMessages) {
                                MockDataStub.createContact()
                            } else {
                                contactsRepository.findOrCreateContact(chat.walletAddress)
                            },
                            isOnline = true,
                            resourceManager = resourceManager,
                        )
                    }
            )
        }
    }

    fun onChatClicked(chat: ChatItemViewHolderItem) {
        tariNavigator.navigate(Navigation.Chat.ToChat(chat.walletAddress, false))
    }

    fun onAddChatClicked() {
        tariNavigator.navigate(Navigation.Chat.ToAddChat)
    }
}