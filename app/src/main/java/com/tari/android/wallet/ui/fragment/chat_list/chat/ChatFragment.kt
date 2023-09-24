package com.tari.android.wallet.ui.fragment.chat_list.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentChatBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.AdapterFactory
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.chat_list.chat.cells.MessageCellViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

class ChatFragment : CommonFragment<FragmentChatBinding, ChatViewModel>() {

    private val adapter = AdapterFactory.generate<CommonViewHolderItem>(MessageCellViewHolder.getBuilder())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChatBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChatViewModel by viewModels()
        bindViewModel(viewModel)

        val walletAddress = arguments?.parcelable<TariWalletAddress>(WALLET_ADDRESS)!!
        viewModel.startWith(walletAddress)

        initUI()
        observeUI()
    }

    private fun initUI() {
        ui.list.adapter = adapter
        ui.list.layoutManager = LinearLayoutManager(requireContext())

        ui.sendTariButton.setOnClickListener {
            viewModel.sendMessage(ui.messageInput.ui.editText.text.toString())
            ui.messageInput.setText("")
        }

        ui.attachButton.setOnClickListener { viewModel.showOptions() }
    }

    private fun observeUI() = with(viewModel) {
        observe(contact) { showContact(it) }

        observe(messages) {
            adapter.update(it)
            ui.emptyState.setVisible(it.isEmpty())
            ui.list.setVisible(it.isNotEmpty())
        }
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

