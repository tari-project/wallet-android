package com.tari.android.wallet.ui.fragment.contact_book.transactionHistory

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
import com.tari.android.wallet.ui.common.recyclerView.AdapterFactory
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.TitleViewHolder
import com.tari.android.wallet.ui.extension.serializable
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.TariNavigator
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListViewHolder
import com.tari.android.wallet.util.extractEmojis
import yat.android.ui.extension.HtmlHelper

class TransactionHistoryFragment : CommonFragment<FragmentContactTransactionHistoryBinding, TransactionHistoryViewModel>() {

    private var adapter = AdapterFactory.generate<CommonViewHolderItem>(TitleViewHolder.getBuilder(), TxListViewHolder.getBuilder())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentContactTransactionHistoryBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: TransactionHistoryViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.selectedContact.postValue(requireArguments().serializable(TariNavigator.PARAMETER_CONTACT)!!)

        initUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(list) { updateList(it) }

        observe(selectedContact) { setContactText(it) }
    }

    private fun updateList(items: MutableList<CommonViewHolderItem>) {
        ui.list.setVisible(items.isNotEmpty())
        ui.descriptionView.setVisible(items.isNotEmpty())
        ui.emptyState.setVisible(items.isEmpty())

        adapter.update(items)
    }

    private fun initUI() = with(ui) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            if (item is TransactionItem) {
                viewModel.navigation.postValue(Navigation.TxListNavigation.ToTxDetails(item.tx))
            }
        })

        sendTariButton.setOnClickListener {
            viewModel.navigation.postValue(Navigation.TxListNavigation.ToSendTariToUser(viewModel.selectedContact.value!!))
        }
    }

    private fun setContactText(contactDto: ContactDto) {
        val name = contactDto.contact.getAlias().ifBlank {
            contactDto.contact.extractWalletAddress().let {
                val emojiId = it.emojiId.extractEmojis()
                val shortEmojiId = emojiId.take(3) + getString(R.string.emoji_id_bullet_separator) + emojiId.takeLast(3)
                shortEmojiId.joinToString("")
            }
        }
        val text = getString(R.string.contact_details_transaction_history_description, name)
        ui.descriptionView.text = text

        val emptyStateText = getString(R.string.contact_details_transaction_history_empty_state_description, name)
        ui.emptyStateDescription.text = HtmlHelper.getSpannedText(emptyStateText)

    }

    companion object {
        fun createFragment(args: ContactDto): TransactionHistoryFragment = TransactionHistoryFragment().apply {
            arguments = Bundle().apply { putSerializable(TariNavigator.PARAMETER_CONTACT, args) }
        }
    }
}