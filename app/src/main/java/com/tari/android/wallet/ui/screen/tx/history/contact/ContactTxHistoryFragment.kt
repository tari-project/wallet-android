package com.tari.android.wallet.ui.screen.tx.history.contact

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.databinding.FragmentContactTransactionHistoryBinding
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.screen.tx.adapter.TxListAdapter
import com.tari.android.wallet.ui.screen.tx.adapter.TxViewHolderItem
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.visible
import yat.android.ui.extension.HtmlHelper

class ContactTxHistoryFragment : CommonXmlFragment<FragmentContactTransactionHistoryBinding, ContactTxHistoryViewModel>() {

    private var adapter = TxListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactTransactionHistoryBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ContactTxHistoryViewModel by viewModels()
        bindViewModel(viewModel)

        initUI()
        observeUI()
    }

    private fun observeUI() {
        collectFlow(viewModel.uiState) { uiState ->
            ui.list.setVisible(uiState.txList.isNotEmpty())
            ui.descriptionViewContainer.setVisible(uiState.txList.isNotEmpty())
            ui.emptyState.setVisible(uiState.txList.isEmpty())

            adapter.update(uiState.txList)
            adapter.notifyDataSetChanged()
        }
    }

    private fun initUI() = with(ui) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            if (item is TxViewHolderItem) {
                viewModel.onTransactionClick(item.txDto.tx)
            }
        })

        sendTariButton.setOnClickListener {
            viewModel.onSendTariClick()
        }

        setContactText(viewModel.uiState.value.selectedContact)
    }

    private fun setContactText(contactDto: ContactDto) {
        val name = contactDto.contactInfo.getAlias()
        val address = contactDto.walletAddress

        ui.descriptionView.text = getString(R.string.contact_details_transaction_history_description, name)
        if (name.isBlank() && address != null) {
            ui.emojiIdViewContainer.root.visible()
            ui.emojiIdViewContainer.textViewEmojiPrefix.text = address.addressPrefixEmojis()
            ui.emojiIdViewContainer.textViewEmojiFirstPart.text = address.addressFirstEmojis()
            ui.emojiIdViewContainer.textViewEmojiLastPart.text = address.addressLastEmojis()
        } else {
            ui.emojiIdViewContainer.root.gone()
        }

        val emptyStateText = getString(R.string.contact_details_transaction_history_empty_state_description, name)
        ui.emptyStateDescription.text = HtmlHelper.getSpannedText(emptyStateText)
    }

    companion object {
        fun createFragment(args: ContactDto): ContactTxHistoryFragment = ContactTxHistoryFragment().apply {
            arguments = Bundle().apply { putParcelable(TariNavigator.PARAMETER_CONTACT, args) }
        }
    }
}