package com.tari.android.wallet.ui.fragment.tx.history.all

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentAllTransactionHistoryBinding
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.tx.adapter.TxListAdapter
import com.tari.android.wallet.ui.fragment.tx.adapter.TxViewHolderItem

class AllTxHistoryFragment : CommonFragment<FragmentAllTransactionHistoryBinding, AllTxHistoryViewModel>() {

    private var adapter = TxListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentAllTransactionHistoryBinding.inflate(inflater, container, false).apply { ui = this }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AllTxHistoryViewModel by viewModels()
        bindViewModel(viewModel)

        initUI()
        observeUI()
    }

    private fun observeUI() = with(viewModel) {
        collectFlow(uiState) { state ->
            ui.searchFullContainer.setVisible(state.searchBarVisible)
            ui.emptyState.setVisible(state.txEmptyStateVisible)
            ui.list.setVisible(state.txListVisible)
            adapter.update(state.sortedTxList)
            adapter.notifyDataSetChanged()
            ui.list.smoothScrollToPosition(0)
        }
    }

    private fun initUI() = with(ui) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        requestTariButton.setOnClickListener { viewModel.onRequestTariClick() }

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            if (item is TxViewHolderItem) {
                viewModel.onTransactionClick(item.txDto.tx)
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