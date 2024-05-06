package com.tari.android.wallet.ui.fragment.contact_book.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionViewModel.ContinueButtonEffect

class AddContactFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.contact_book_add_contact_title)
        ui.addFirstNameInput.visible()

        viewModel.additionalFilter = { it.contact.getFFIDto() != null && it.contact.contact.getAlias().isEmpty() }
    }

    override fun goToNext() {
        super.goToNext()

        viewModel.onContinueButtonClick(ContinueButtonEffect.AddContact(name = ui.addFirstNameInput.ui.editText.text.toString()))
    }
}