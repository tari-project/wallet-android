package com.tari.android.wallet.ui.fragment.utxos.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.databinding.FragmentUtxosListBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosListAdapter
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosListTileAdapter
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.CheckedController
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListTypeSwitchController

class UtxosListFragment : CommonFragment<FragmentUtxosListBinding, UtxosListViewModel>() {

    private lateinit var listTypeSwitchController: ListTypeSwitchController
    private lateinit var selectionController: CheckedController

    private val textListAdapter: UtxosListAdapter = UtxosListAdapter()
    private val tileLeftAdapter: UtxosListTileAdapter = UtxosListTileAdapter()
    private val tileRightAdapter: UtxosListTileAdapter = UtxosListTileAdapter()

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
        observeOnLoad(sortingMediator)
        observe(listType) {
            when (it!!) {
                ListType.Text -> {
                    ui.utxosTextList.visible()
                    ui.tileContainer.gone()
                }
                ListType.Tile -> {
                    ui.utxosTextList.gone()
                    ui.tileContainer.visible()
                }
            }
        }
        observe(ordering) { ui.orderingState.setText(it.textId) }
        observe(textList) { textListAdapter.update(it) }
        observe(leftTileList) { tileLeftAdapter.update(it) }
        observe(rightTileList) { tileRightAdapter.update(it) }
    }

    private fun setupCTA() {
        ui.backCtaView.setOnClickListener { requireActivity().onBackPressed() }
        ui.orderingState.setOnClickListener { viewModel.showOrderingSelectionDialog() }
    }

    private fun setupUI() {
        listTypeSwitchController = ListTypeSwitchController(ui.typeListSelector)
        listTypeSwitchController.toggleCallback = { viewModel.setTypeList(it) }
        listTypeSwitchController.set(ListType.Tile)

        selectionController = CheckedController(ui.selectingState)
        selectionController.toggleCallback = { viewModel.setSelectionState(it) }
        ui.selectingState.setOnClickListener { selectionController.toggleChecked() }
        selectionController.setChecked(false)

        ui.utxosTextList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTextList.adapter = textListAdapter

        ui.utxosTileLeftList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTileLeftList.adapter = tileLeftAdapter

        ui.utxosTileRightList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTileRightList.adapter = tileRightAdapter

        syncroniseTileScrolling()
    }

    private fun syncroniseTileScrolling() {
        val scrollListeners = mutableListOf<RecyclerView.OnScrollListener>()

        val leftListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                ui.utxosTileRightList.removeOnScrollListener(scrollListeners[1])
                ui.utxosTileRightList.scrollBy(dx, dy)
                ui.utxosTileRightList.addOnScrollListener(scrollListeners[1])
            }
        }
        val rightListener = object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                ui.utxosTileLeftList.removeOnScrollListener(scrollListeners[0])
                ui.utxosTileLeftList.scrollBy(dx, dy)
                ui.utxosTileLeftList.addOnScrollListener(scrollListeners[0])
            }
        }
        scrollListeners.add(leftListener)
        scrollListeners.add(rightListener)
        ui.utxosTileLeftList.addOnScrollListener(leftListener)
        ui.utxosTileRightList.addOnScrollListener(rightListener)
    }
}