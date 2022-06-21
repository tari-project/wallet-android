package com.tari.android.wallet.ui.fragment.utxos.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentUtxosListBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosListAdapter
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.CheckedController
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListTypeSwitchController
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderDirection
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderType
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ordering.OrderingController

class UtxosListFragment : CommonFragment<FragmentUtxosListBinding, UtxosListViewModel>() {

    private lateinit var listTypeSwitchController: ListTypeSwitchController
    private lateinit var orderingController: OrderingController
    private lateinit var selectionController: CheckedController

    private val textListAdapter: UtxosListAdapter = UtxosListAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentUtxosListBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: UtxosListViewModel by viewModels()
        bindViewModel(viewModel)

        observeUI()
        setupCTA()
        setupUI()
    }

    private fun observeUI() = with(viewModel) {
        observe(listType) { }
        observe(textList) { textListAdapter.update(it) }
    }

    private fun setupCTA() {
        ui.backCtaView.setOnClickListener { requireActivity().onBackPressed() }
    }

    private fun setupUI() {
        listTypeSwitchController = ListTypeSwitchController(CheckedController(ui.groupSelectorGroups), CheckedController(ui.groupSelectorList))
        listTypeSwitchController.toggleCallback = { viewModel.setTypeList(it) }
        listTypeSwitchController.toggle(ListType.Text)

        orderingController = OrderingController(ui.orderingTypeValue, ui.orderingTypeDate, ui.orderingDirection)
        orderingController.toggleTypeCallback = { viewModel.setOrderingType(it) }
        orderingController.toggleDirectionCallback = { viewModel.setOrderingDirection(it) }
        orderingController.toggleType(OrderType.ByValue)
        orderingController.toggleDirection(OrderDirection.Anc)

        selectionController = CheckedController(ui.startSelectionChecker)
        selectionController.toggleCallback = { viewModel.setSelectionState(it) }
        ui.startSelectionChecker.setOnClickListener { selectionController.toggleChecked() }
        selectionController.setChecked(false)

        ui.utxosTextList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTextList.adapter = textListAdapter
    }
}