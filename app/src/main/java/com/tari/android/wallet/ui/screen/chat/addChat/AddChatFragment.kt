package com.tari.android.wallet.ui.screen.chat.addChat

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.ui.screen.contactBook.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.screen.contactBook.contactSelection.ContactSelectionViewModel.ContinueButtonEffect

class AddChatFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.chat_add_chat)
        ui.addFirstNameInput.gone()

        viewModel.additionalFilter = { it.contact.getFFIContactInfo() != null }
    }

    override fun goToNext() {
        super.goToNext()

        viewModel.onContinueButtonClick(ContinueButtonEffect.AddChat)
    }
}