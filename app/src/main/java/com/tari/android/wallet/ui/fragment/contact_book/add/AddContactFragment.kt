package com.tari.android.wallet.ui.fragment.contact_book.add

import android.os.Bundle
import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.contactSelection.ContactSelectionFragment
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation

class AddContactFragment : ContactSelectionFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ui.toolbar.ui.toolbarTitle.text = string(R.string.contact_book_add_contact_title)

        viewModel.additionalFilter = { it.contact.contact is FFIContactDto && it.contact.contact.getAlias().isEmpty() }
    }

    override fun goToNext() {
        super.goToNext()

        val user = viewModel.getUserDto()
        viewModel.navigation.postValue(Navigation.ContactBookNavigation.ToAddContactName(user))
    }
}