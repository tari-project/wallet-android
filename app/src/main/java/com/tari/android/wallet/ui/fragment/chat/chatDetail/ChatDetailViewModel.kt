package com.tari.android.wallet.ui.fragment.chat.chatDetail

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.chat.chatDetail.ChatDetailModel.WALLET_ADDRESS
import com.tari.android.wallet.ui.fragment.chat.chatDetail.adapter.ChatMessageViewHolderItem
import com.tari.android.wallet.ui.fragment.chat.data.ChatMessageItemDto
import com.tari.android.wallet.ui.fragment.chat.data.ChatsRepository
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

class ChatDetailViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var chatRepository: ChatsRepository

    val walletAddress = savedState.get<TariWalletAddress>(WALLET_ADDRESS)!!

    init {
        component.inject(this)
    }

    // should be after the init block
    private val _uiState = MutableStateFlow(
        ChatDetailModel.UiState(
            walletAddress = walletAddress,
            contact = contactsRepository.getContactByAddress(walletAddress),
            messages = chatRepository.getChatByWalletAddress(walletAddress)?.messages.orEmpty().map { ChatMessageViewHolderItem(it) }
        )
    )
    val uiState: StateFlow<ChatDetailModel.UiState> = _uiState.asStateFlow()

    fun sendMessage(message: String) {
        //todo send to backend
        val dto = ChatMessageItemDto(UUID.randomUUID().toString(), message, "date", true, false)
        chatRepository.addMessage(uiState.value.walletAddress, dto)
        _uiState.update {
            it.copy(messages = it.messages + ChatMessageViewHolderItem(dto))
        }
    }

    fun showOptions() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.chat_options_title)),
            ButtonModule(resourceManager.getString(R.string.send_tari), ButtonStyle.Normal, ::onSendTariClicked),
            ButtonModule(resourceManager.getString(R.string.request_tari), ButtonStyle.Normal, ::onRequestTariClicked),
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close),
        )
    }

    private fun onSendTariClicked() {
        hideDialog()
        navigation.postValue(Navigation.ContactBookNavigation.ToSendTari(uiState.value.contact))
    }

    private fun onRequestTariClicked() {
        hideDialog()
        navigation.postValue(Navigation.AllSettingsNavigation.ToRequestTari)
    }

}