package com.tari.android.wallet.ui.screen.contactBook.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.databinding.FragmentContactsDetailsBinding
import com.tari.android.wallet.navigation.TariNavigator.Companion.PARAMETER_CONTACT
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.screen.contactBook.details.adapter.ContactDetailsAdapter
import com.tari.android.wallet.util.extension.collectFlow

class ContactDetailsFragment : CommonXmlFragment<FragmentContactsDetailsBinding, ContactDetailsViewModel>() {

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
        this@ContactDetailsFragment.collectFlow(uiState) { uiState ->
            applyContact(uiState.contact)
            adapter.update(uiState.viewHolderItemList)
        }
    }

    private fun applyContact(contact: Contact) {
//        if (contact.getContactActions().contains(ContactAction.EditName)) {
//            ui.toolbar.setRightArgs(TariToolbarActionArg(title = getString(R.string.contact_book_details_edit)) { viewModel.onEditClick() })
//        } else if (contact.getContactActions().contains(ContactAction.AddContact)) {
//            ui.toolbar.setRightArgs(TariToolbarActionArg(title = getString(R.string.contact_book_add)) { viewModel.onEditClick() })
//        } else {
//            ui.toolbar.hideRightActions()
//        }
        ui.toolbar.hideRightActions()
    }

    private fun setupUI() = with(ui) {
        listUi.layoutManager = LinearLayoutManager(requireContext())
        listUi.adapter = adapter
    }

    companion object {

        fun createFragment(args: Contact): ContactDetailsFragment = ContactDetailsFragment().apply {
            arguments = Bundle().apply { putParcelable(PARAMETER_CONTACT, args) }
        }
    }
}
