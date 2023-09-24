package com.tari.android.wallet.ui.fragment.chat_list.chat

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import javax.inject.Inject

class ChatViewModel: CommonViewModel() {

    val userAddress = MutableLiveData<TariWalletAddress>()

    val contact = MutableLiveData<ContactDto>()

    init {
        component.inject(this)
    }

    @Inject
    lateinit var contactsRepository: ContactsRepository

    fun startWith(walletAddress: TariWalletAddress) {
        userAddress.postValue(walletAddress)

        val contact = contactsRepository.ffiBridge.getContactByAddress(walletAddress)
        this.contact.postValue(contact)
    }
}