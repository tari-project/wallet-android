package com.tari.android.wallet.ui.fragment.contact_book.addContactName

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentContactsAddNameBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.fullEmojiId.FullEmojiIdViewController
import com.tari.android.wallet.ui.extension.serializable
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookNavigation
import com.tari.android.wallet.ui.fragment.contact_book.root.ContactBookRouter
import com.tari.android.wallet.ui.fragment.home.HomeActivity

class AddContactNameFragment : CommonFragment<FragmentContactsAddNameBinding, AddContactNameViewModel>() {

    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactsAddNameBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AddContactNameViewModel by viewModels()
        bindViewModel(viewModel)

        val contact = requireArguments().serializable<ContactDto>(HomeActivity.PARAMETER_CONTACT)!!
        viewModel.initContact(contact)

        setupUI()

        subscribeViewModal()
    }

    private fun subscribeViewModal() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(contact) { contact -> displayEmojiId((contact.contact as FFIContactDto).walletAddress) }
    }

    private fun processNavigation(navigation: ContactBookNavigation) {
        ContactBookRouter.processNavigation(requireActivity(), navigation)
    }

    private fun setupUI() = with(ui) {
        toolbar.rightAction = { viewModel.onContinue(addNameInput.ui.editText.text.toString()) }

        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            requireContext(),
            null
        )
        emojiIdSummaryContainerView.setOnClickListener { emojiIdClicked() }
    }

    private fun emojiIdClicked() {
        fullEmojiIdViewController.showFullEmojiId()
    }

    private fun displayEmojiId(emojiId: TariWalletAddress) {
        fullEmojiIdViewController.fullEmojiId = emojiId.emojiId
        fullEmojiIdViewController.emojiIdHex = emojiId.hexString
        ui.emojiIdSummaryContainerView.visible()
        emojiIdSummaryController.display(emojiId.emojiId)
    }

    companion object {
        fun createFragment(contact: ContactDto): Fragment = AddContactNameFragment().apply {
            arguments = Bundle().apply { putSerializable(HomeActivity.PARAMETER_CONTACT, contact) }
        }
    }
}

