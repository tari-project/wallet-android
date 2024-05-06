package com.tari.android.wallet.ui.fragment.chat_list

import androidx.lifecycle.MediatorLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.chat_list.adapter.ChatItemViewHolderItem
import com.tari.android.wallet.ui.fragment.chat_list.data.ChatsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.util.extractEmojis
import javax.inject.Inject

class ChatListViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    @Inject
    lateinit var chatsRepository: ChatsRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    val list = MediatorLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)

        list.addSource(chatsRepository.list) { chats ->
            list.postValue(chats.map { chat ->
                val firstEmoji = chat.walletAddress.emojiId.extractEmojis().first()
                val contact = contactsRepository.getContactByAddress(chat.walletAddress)
                val lastMessage = chat.messages.maxByOrNull { it.date }
                val unreadCount = chat.messages.count { it.isRead.not() }
                ChatItemViewHolderItem(
                    walletAddress = chat.walletAddress,
                    uuid = chat.uuid,
                    firstEmoji = firstEmoji,
                    avatar = contact.getPhoneDto()?.avatar.orEmpty(),
                    alias = contact.contact.getAlias(),
                    emojiId = chat.walletAddress.emojiId,
                    subtitle = lastMessage?.message.orEmpty(),
                    dateMessage = lastMessage?.date.orEmpty(),
                    unreadCount = unreadCount,
                    isOnline = true,
                )
            }.toMutableList())
        }
    }
}