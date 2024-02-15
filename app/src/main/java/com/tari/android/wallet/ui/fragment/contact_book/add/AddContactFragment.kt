package com.tari.android.wallet.ui.fragment.contact_book.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation

class AddContactFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.contact_book_add_contact_title)
        ui.addFirstNameInput.visible()

        viewModel.additionalFilter = { it.contact.getFFIDto() != null && it.contact.contact.getAlias().isEmpty() }
    }

    override fun goToNext() {
        super.goToNext()

        val user = viewModel.getUserDto()
        val fullName = ui.addFirstNameInput.ui.editText.text.toString()
        val split = fullName.split(" ")
        val firstName = split.getOrNull(1).orEmpty().trim()
        val surname = split.getOrNull(0).orEmpty().trim()

        viewModel.contactsRepository.updateContactInfo(user, firstName, surname, "")
        viewModel.navigation.postValue(Navigation.ContactBookNavigation.BackToContactBook)
    }
}