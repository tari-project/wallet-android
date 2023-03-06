package com.tari.android.wallet.ui.fragment.contact_book.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentContactsDetailsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.serializable
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.ContactDetailsAdapter
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookRouter
import com.tari.android.wallet.ui.fragment.home.HomeActivity.Companion.PARAMETER_CONTACT

class ContactDetailsFragment : CommonFragment<FragmentContactsDetailsBinding, ContactDetailsViewModel>() {

    private val adapter = ContactDetailsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsDetailsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactDetailsViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.initArgs(requireArguments().serializable(PARAMETER_CONTACT)!!)

        setupUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(list) { adapter.update(it) }

        observe(navigation) { ContactBookRouter.processNavigation(requireActivity(), it) }

        observe(contact) { applyContact(it) }
    }

    private fun applyContact(contact: ContactDto) {
        if (contact.getContactActions().contains(ContactAction.EditName)) {
            ui.toolbar.setupRightButton(getString(R.string.contact_book_details_edit))
        } else {
            ui.toolbar.clearRightIcon()
        }
    }

    private fun setupUI() = with(ui) {
        listUi.layoutManager = LinearLayoutManager(requireContext())
        listUi.adapter = adapter
        toolbar.rightAction = { viewModel.onEditClick() }
    }

    companion object {

        fun createFragment(args: ContactDto): ContactDetailsFragment = ContactDetailsFragment().apply {
            arguments = Bundle().apply { putSerializable(PARAMETER_CONTACT, args) }
        }
    }
}
