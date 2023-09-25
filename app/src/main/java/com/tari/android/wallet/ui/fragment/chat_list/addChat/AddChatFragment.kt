package com.tari.android.wallet.ui.fragment.chat_list.addChat

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.chat_list.data.ChatItemDto
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import java.util.UUID

class AddChatFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.chat_add_chat)
        ui.addFirstNameInput.gone()
        ui.addSurnameInput.gone()

        viewModel.additionalFilter = { it.contact.getFFIDto() != null }
    }

    override fun goToNext() {
        super.goToNext()

        val user = viewModel.getUserDto()

        val chatDto = ChatItemDto(UUID.randomUUID().toString(), listOf(), user.getFFIDto()!!.walletAddress)
        viewModel.chatsRepository.addChat(chatDto)

        viewModel.navigation.postValue(Navigation.ChatNavigation.ToChat(user.getFFIDto()?.walletAddress!!, true))
    }
}