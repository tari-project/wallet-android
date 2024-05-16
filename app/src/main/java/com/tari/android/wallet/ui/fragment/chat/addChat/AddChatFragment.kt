package com.tari.android.wallet.ui.fragment.chat.addChat

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionViewModel.ContinueButtonEffect

class AddChatFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.chat_add_chat)
        ui.addFirstNameInput.gone()

        viewModel.additionalFilter = { it.contact.getFFIDto() != null }
    }

    override fun goToNext() {
        super.goToNext()

        viewModel.onContinueButtonClick(ContinueButtonEffect.AddChat)
    }
}