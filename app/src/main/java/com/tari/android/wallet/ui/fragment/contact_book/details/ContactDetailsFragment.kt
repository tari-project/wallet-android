package com.tari.android.wallet.ui.fragment.contact_book.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentContactsDetailsBinding
import com.tari.android.wallet.databinding.ViewEmojiIdWithYatSummaryBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.fullEmojiId.FullEmojiIdViewController
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.doOnGlobalLayout
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.serializable
import com.tari.android.wallet.ui.extension.setLayoutHeight
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactAction
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.ContactDetailsAdapter
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator.Companion.PARAMETER_CONTACT

class ContactDetailsFragment : CommonFragment<FragmentContactsDetailsBinding, ContactDetailsViewModel>() {

    private val adapter = ContactDetailsAdapter()

    private var fullEmojiIdViewController: FullEmojiIdViewController? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsDetailsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactDetailsViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.initArgs(requireArguments().serializable(PARAMETER_CONTACT)!!)

        setupUI()
        observeUI()
        ui.emojiIdOuterContainer.root.gone()
    }

    private fun observeUI() = with(viewModel) {
        observe(list) { adapter.update(it) }

        observe(contact) { applyContact(it) }

        observe(showFullEmojiId) { showFullEmojiId() }

        observe(initFullEmojiId) { initFullEmojiId(it) }
    }

    private fun initFullEmojiId(binding: ViewEmojiIdWithYatSummaryBinding) {
        if (fullEmojiIdViewController == null) {
            fullEmojiIdViewController = FullEmojiIdViewController(
                ui.emojiIdOuterContainer,
                binding.emojiIdSummaryView,
                requireContext()
            )
            viewModel.contact.value?.contact?.extractWalletAddress()?.let {
                fullEmojiIdViewController?.fullEmojiId = it.emojiId
                fullEmojiIdViewController?.emojiIdHex = it.hexString
            }
        }

        ui.root.doOnGlobalLayout {
            ui.emojiIdOuterContainer.fullEmojiIdContainerView.apply {
                setTopMargin(binding.root.top)
                setLayoutHeight(binding.root.height)
                setLayoutWidth(binding.root.width)
            }
        }
    }

    private fun showFullEmojiId() {
        fullEmojiIdViewController?.showFullEmojiId()
    }

    private fun applyContact(contact: ContactDto) {
        val address = contact.contact.extractWalletAddress()
        fullEmojiIdViewController?.fullEmojiId = address.emojiId
        fullEmojiIdViewController?.emojiIdHex = address.hexString

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
            arguments = Bundle().apply { putSerializable(PARAMETER_CONTACT, args) }
        }
    }
}
