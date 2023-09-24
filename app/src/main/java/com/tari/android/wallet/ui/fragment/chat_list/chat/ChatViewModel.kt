package com.tari.android.wallet.ui.fragment.chat_list.chat

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.chat_list.data.ChatsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import javax.inject.Inject

class ChatViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var chatRepository: ChatsRepository

    val userAddress = MutableLiveData<TariWalletAddress>()

    val contact = MutableLiveData<ContactDto>()

    val messages = MutableLiveData<List<MessagePresentationDto>>()

    init {
        component.inject(this)
    }


    fun startWith(walletAddress: TariWalletAddress) {
        userAddress.postValue(walletAddress)

        val contact = contactsRepository.ffiBridge.getContactByAddress(walletAddress)
        this.contact.postValue(contact)

        val chat = chatRepository.getByWalletAddress(walletAddress)

        val messagesPresentation = chat?.messages.orEmpty().map {
            MessagePresentationDto(it.message, it.isMine)
        }

        this.messages.postValue(messagesPresentation)
    }
}