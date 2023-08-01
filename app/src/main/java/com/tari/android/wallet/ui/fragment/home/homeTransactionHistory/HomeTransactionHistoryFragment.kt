package com.tari.android.wallet.ui.fragment.home.homeTransactionHistory

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentHomeContactTransactionHistoryBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
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
        observe(list) { updateList(it) }
    }

    private fun updateList(items: MutableList<CommonViewHolderItem>) {
        ui.list.setVisible(items.isNotEmpty())
        ui.searchFullContainer.setVisible(items.isNotEmpty())
//        ui.descriptionView.setVisible(items.isNotEmpty())
        ui.emptyState.setVisible(items.isEmpty())

        adapter.update(items)
    }

    private fun initUI() = with(ui) {
        list.adapter = adapter
        list.layoutManager = LinearLayoutManager(context)
        requestTariButton.setOnClickListener { viewModel.navigation.postValue(Navigation.AllSettingsNavigation.ToRequestTari) }

        toolbar.setRightArgs(TariToolbarActionArg(icon = R.drawable.vector_wallet) {
            viewModel.navigation.postValue(Navigation.TxListNavigation.ToUtxos)
        })

        adapter.setClickListener(CommonAdapter.ItemClickListener { item ->
            if (item is TransactionItem) {
                viewModel.navigation.postValue(Navigation.TxListNavigation.ToTxDetails(item.tx))
            }
        })
    }
}