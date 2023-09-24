package com.tari.android.wallet.ui.fragment.chat_list.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentChatBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

class ChatFragment : CommonFragment<FragmentChatBinding, ChatViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChatBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChatViewModel by viewModels()
        bindViewModel(viewModel)

        val walletAddress = arguments?.parcelable<TariWalletAddress>(WALLET_ADDRESS)!!
        viewModel.startWith(walletAddress)

        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(contact) { showContact(it) }
    }

    private fun showContact(contact: ContactDto) {
        ui.toolbar.setText(contact.contact.getAlias())
    }

    companion object {
        const val WALLET_ADDRESS = "Wallet address key"

        fun newInstance(walletAddress: TariWalletAddress) = ChatFragment().apply {
            arguments = Bundle().apply {
                putParcelable(WALLET_ADDRESS, walletAddress)
            }
        }
    }
}

