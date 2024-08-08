package com.tari.android.wallet.ui.fragment.contactBook.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentContactsDetailsBinding
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactAction
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.details.adapter.ContactDetailsAdapter
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_CONTACT

class ContactDetailsFragment : CommonFragment<FragmentContactsDetailsBinding, ContactDetailsViewModel>() {

    private val adapter = ContactDetailsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsDetailsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactDetailsViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        collectFlow(uiState) { uiState ->
            applyContact(uiState.contact)
            adapter.update(uiState.list)
        }
    }

    private fun applyContact(contact: ContactDto) {
        if (contact.getContactActions().contains(ContactAction.EditName)) {
            ui.toolbar.setRightArgs(TariToolbarActionArg(title = getString(R.string.contact_book_details_edit)) {
                viewModel.onEditClick()
            })
        } else {
            ui.toolbar.hideRightActions()
        }
    }

    private fun setupUI() = with(ui) {
        listUi.layoutManager = LinearLayoutManager(requireContext())
        listUi.adapter = adapter
    }

    companion object {

        fun createFragment(args: ContactDto): ContactDetailsFragment = ContactDetailsFragment().apply {
            arguments = Bundle().apply { putParcelable(PARAMETER_CONTACT, args) }
        }
    }
}
