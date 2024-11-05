package com.tari.android.wallet.ui.fragment.chat.chatList

import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.chat.chatList.adapter.ChatItemViewHolderItem
import com.tari.android.wallet.ui.fragment.chat.data.ChatItemDto
import com.tari.android.wallet.ui.fragment.chat.data.ChatsRepository
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
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
                                contactsRepository.getContactByAddress(chat.walletAddress)
                            },
                            isOnline = contactsRepository.isContactOnline(chat.walletAddress),
                            resourceManager = resourceManager,
                        )
                    }
            )
        }
    }

    fun onChatClicked(chat: ChatItemViewHolderItem) {
        navigation.postValue(Navigation.ChatNavigation.ToChat(chat.walletAddress, false))
    }

    fun onAddChatClicked() {
        navigation.postValue(Navigation.ChatNavigation.ToAddChat)
    }
}