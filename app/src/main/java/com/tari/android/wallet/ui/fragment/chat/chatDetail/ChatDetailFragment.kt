package com.tari.android.wallet.ui.fragment.chat.chatDetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentChatBinding
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.AdapterFactory
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.chat.chatDetail.ChatDetailModel.WALLET_ADDRESS
import com.tari.android.wallet.ui.fragment.chat.chatDetail.adapter.ChatMessageViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto

class ChatDetailFragment : CommonFragment<FragmentChatBinding, ChatDetailViewModel>() {

    private val adapter = AdapterFactory.generate<CommonViewHolderItem>(ChatMessageViewHolder.getBuilder())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChatBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChatDetailViewModel by viewModels()
        bindViewModel(viewModel)

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

    private fun observeUI() {
        collectFlow(viewModel.uiState) { uiState ->
            showContact(uiState.contact)

            adapter.update(uiState.messages)
            ui.emptyState.setVisible(uiState.showEmptyState)
            ui.list.setVisible(!uiState.showEmptyState)
        }
    }

    private fun showContact(contact: ContactDto) {
        ui.toolbar.setText(contact.contact.getAlias())
    }

    companion object {
        fun newInstance(walletAddress: TariWalletAddress) = ChatDetailFragment().apply {
            arguments = Bundle().apply {
                putParcelable(WALLET_ADDRESS, walletAddress)
            }
        }
    }
}

