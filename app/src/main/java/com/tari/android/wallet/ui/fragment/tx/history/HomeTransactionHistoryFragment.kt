package com.tari.android.wallet.ui.fragment.tx.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentHomeContactTransactionHistoryBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListAdapter

class HomeTransactionHistoryFragment : CommonFragment<FragmentHomeContactTransactionHistoryBinding, HomeTransactionHistoryViewModel>() {

    private var adapter = TxListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentHomeContactTransactionHistoryBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: HomeTransactionHistoryViewModel by viewModels()
        bindViewModel(viewModel)

        initUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(list) {
            adapter.update(it)
            adapter.notifyDataSetChanged()
        }

        observe(searchBarVisible) { ui.searchFullContainer.setVisible(it) }

//        observe(searchEmptyStateVisible) { ui.emptyState.setVisible(it) }

        observe(txEmptyStateVisible) { ui.emptyState.setVisible(it) }

        observe(txListVisible) { ui.list.setVisible(it) }
    }

    private fun initUI() = with(ui) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        requestTariButton.setOnClickListener { viewModel.onRequestTariClick() }

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            if (item is TransactionItem) {
                viewModel.onTransactionClick(item.tx)
            }
        })

        ui.searchView.setIconifiedByDefault(false)

        ui.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.doSearch(newText.orEmpty())
                return true
            }

            override fun onQueryTextSubmit(query: String?): Boolean = true
        })
    }
}