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

        list.addSource(chatsRepository.list) {
            list.postValue(it.map {
                val firstEmoji = it.walletAddress.emojiId.extractEmojis().first()
                val contact = contactsRepository.ffiBridge.getContactByAddress(it.walletAddress)
                val lastMessage = it.messages.sortedBy { it.date }.lastOrNull()
                val unreadCount = it.messages.count { it.isRead.not() }
                ChatItemViewHolderItem(
                    it.walletAddress,
                    it.uuid,
                    firstEmoji,
                    contact.getPhoneDto()?.avatar.orEmpty(),
                    contact.contact.getAlias(),
                    it.walletAddress.emojiId,
                    lastMessage?.message.orEmpty(),
                    lastMessage?.date.orEmpty(),
                    unreadCount,
                    true
                )
            }.toMutableList())
        }
    }
}