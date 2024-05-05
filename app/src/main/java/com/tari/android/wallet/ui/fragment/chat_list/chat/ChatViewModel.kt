package com.tari.android.wallet.ui.fragment.chat_list.chat

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.chat_list.chat.cells.MessagePresentationDto
import com.tari.android.wallet.ui.fragment.chat_list.data.ChatMessageItemDto
import com.tari.android.wallet.ui.fragment.chat_list.data.ChatsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import java.util.UUID
import javax.inject.Inject

class ChatViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var chatRepository: ChatsRepository

    val userAddress = MutableLiveData<TariWalletAddress>()

    val contact = MutableLiveData<ContactDto>()

    val messages = MutableLiveData<MutableList<CommonViewHolderItem>>()

    init {
        component.inject(this)
    }


    fun startWith(walletAddress: TariWalletAddress) {
        userAddress.postValue(walletAddress)

        val contact = contactsRepository.getContactByAddress(walletAddress)
        this.contact.postValue(contact)

        val chat = chatRepository.getByWalletAddress(walletAddress)

        val list = mutableListOf<CommonViewHolderItem>()

        val messagesPresentation = chat?.messages.orEmpty().map { getMessage(it) }
        list.addAll(messagesPresentation)

        this.messages.postValue(list)
    }

    private fun getMessage(dto: ChatMessageItemDto): MessagePresentationDto = MessagePresentationDto(dto.message, dto.isMine)

    fun sendMessage(message: String) {
        //todo send to backend
        val list = messages.value.orEmpty().toMutableList()
        val dto = ChatMessageItemDto(UUID.randomUUID().toString(), message, "date", true, false)
        chatRepository.addMessage(userAddress.value!!, dto)
        list.add(getMessage(dto))
        messages.postValue(list)
    }

    fun showOptions() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.chat_options_title)),
                ButtonModule(resourceManager.getString(R.string.send_tari), ButtonStyle.Normal) {
                    dismissDialog.postValue(Unit)
                    navigation.postValue(Navigation.ContactBookNavigation.ToSendTari(contact.value!!))
                },
                ButtonModule(resourceManager.getString(R.string.request_tari), ButtonStyle.Normal) {
                    dismissDialog.postValue(Unit)
                    navigation.postValue(Navigation.AllSettingsNavigation.ToRequestTari)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }
}