package com.tari.android.wallet.ui.screen.contactBook.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class ContactDetailsFragment : CommonFragment<ContactDetailsViewModel>() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val state by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ContactDetailsScreen(
                uiState = state,
                onBackClick = { viewModel.onBackPressed() },
                onSendTariClicked = { viewModel.onSendTariClicked() },
                onRequestTariClicked = { viewModel.onRequestTariClicked() },
                onEmojiCopyClick = { viewModel.onEmojiCopyClick() },
                onBase58CopyClick = { viewModel.onBase58CopyClick() },
                onEmojiDetailClick = { viewModel.onAddressDetailsClicked() },
                onTxClick = { tx -> viewModel.onTxClick(tx) },
                onEditAliasClicked = { viewModel.onEditAliasClicked() },
                onShareContactClicked = { viewModel.onShareContactClicked() },
                onDeleteContactClicked = { viewModel.onDeleteContact() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactDetailsViewModel by viewModels()
        bindViewModel(viewModel)
    }

    companion object {
        const val PARAMETER_CONTACT = "PARAMETER_CONTACT"

        fun createFragment(args: Contact): ContactDetailsFragment = ContactDetailsFragment().apply {
            arguments = Bundle().apply { putParcelable(PARAMETER_CONTACT, args) }
        }
    }
}