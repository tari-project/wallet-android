package com.tari.android.wallet.ui.fragment.tx.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentContactTransactionHistoryBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListAdapter
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import yat.android.ui.extension.HtmlHelper

class TransactionHistoryFragment : CommonFragment<FragmentContactTransactionHistoryBinding, TransactionHistoryViewModel>() {

    private var adapter = TxListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactTransactionHistoryBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: TransactionHistoryViewModel by viewModels()
        bindViewModel(viewModel)

        initUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(list) { updateList(it) }
    }

    private fun updateList(items: List<CommonViewHolderItem>) {
        ui.list.setVisible(items.isNotEmpty())
        ui.descriptionViewContainer.setVisible(items.isNotEmpty())
        ui.emptyState.setVisible(items.isEmpty())

        adapter.update(items)
        adapter.notifyDataSetChanged()
    }

    private fun initUI() = with(ui) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            if (item is TransactionItem) {
                viewModel.onTransactionClick(item.tx)
            }
        })

        sendTariButton.setOnClickListener {
            viewModel.onSendTariClick()
        }

        setContactText(viewModel.selectedContact)
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
        fun createFragment(args: ContactDto): TransactionHistoryFragment = TransactionHistoryFragment().apply {
            arguments = Bundle().apply { putParcelable(TariNavigator.PARAMETER_CONTACT, args) }
        }
    }
}