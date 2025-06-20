package com.tari.android.wallet.ui.screen.contactBook.obsolete.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.screen.contactBook.obsolete.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.screen.contactBook.obsolete.contactSelection.ContactSelectionViewModel.ContinueButtonEffect
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.util.extension.visible

class AddContactFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.contact_book_add_contact_title)
        ui.addFirstNameInput.visible()

        viewModel.additionalFilter = { it.contact.alias.isNullOrEmpty() }
    }

    override fun goToNext() {
        super.goToNext()

        viewModel.onContinueButtonClick(ContinueButtonEffect.AddContact(name = ui.addFirstNameInput.ui.editText.text.toString()))
    }
}