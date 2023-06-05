package com.tari.android.wallet.ui.fragment.utxos.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.databinding.FragmentUtxosListBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosListAdapter
import com.tari.android.wallet.ui.fragment.utxos.list.adapters.UtxosListTileAdapter
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.BottomButtonsController
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.CheckedController
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.ScreenState
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListType
import com.tari.android.wallet.ui.fragment.utxos.list.controllers.listType.ListTypeSwitchController

class UtxosListFragment : CommonFragment<FragmentUtxosListBinding, UtxosListViewModel>() {

    private lateinit var listTypeSwitchController: ListTypeSwitchController
    private lateinit var selectionController: CheckedController
    private lateinit var bottomButtonsController: BottomButtonsController

    private val textListAdapter: UtxosListAdapter = UtxosListAdapter()
    private val tileLeftAdapter: UtxosListTileAdapter = UtxosListTileAdapter()
    private val tileRightAdapter: UtxosListTileAdapter = UtxosListTileAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentUtxosListBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: UtxosListViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        observeUI()
        setupCTA()
    }

    private fun observeUI() = with(viewModel) {
        observeOnLoad(sortingMediator)
        observe(listType) { updateListType(it) }
        observe(selectionState) { selectionController.setChecked(it) }
        observe(screenState) { updateState(it) }
        observe(joinSplitButtonsState) { bottomButtonsController.setState(it) }
        observe(ordering) { ui.orderingState.setText(it.textId) }
        observe(textList) { textListAdapter.update(it) }
        observe(leftTileList) { tileLeftAdapter.update(it) }
        observe(rightTileList) { tileRightAdapter.update(it) }
    }

    private fun setupCTA() {
        ui.orderingState.setOnClickListener { viewModel.showOrderingSelectionDialog() }
        ui.joinButton.setOnClickListener { viewModel.join() }
        ui.splitButton.setOnClickListener { viewModel.split() }
    }

    private fun setupUI() {
        listTypeSwitchController = ListTypeSwitchController(ui.tariToolbar)
        listTypeSwitchController.toggleCallback = { viewModel.setTypeList(it) }
        listTypeSwitchController.set(ListType.Tile)

        selectionController = CheckedController(ui.selectingState)
        selectionController.toggleCallback = { viewModel.setSelectionState(it) }
        ui.selectingState.setOnClickListener { selectionController.toggleChecked() }

        ui.utxosTextList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTextList.adapter = textListAdapter
        textListAdapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.selectItem(it) })
        textListAdapter.setLongClickListener(CommonAdapter.ItemLongClickListener { viewModel.setSelectionStateTrue(it) })

        ui.utxosTileLeftList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTileLeftList.adapter = tileLeftAdapter
        tileLeftAdapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.selectItem(it) })
        tileLeftAdapter.setLongClickListener(CommonAdapter.ItemLongClickListener { viewModel.setSelectionStateTrue(it) })

        ui.utxosTileRightList.layoutManager = LinearLayoutManager(requireContext())
        ui.utxosTileRightList.adapter = tileRightAdapter
        tileRightAdapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.selectItem(it) })
        tileRightAdapter.setLongClickListener(CommonAdapter.ItemLongClickListener { viewModel.setSelectionStateTrue(it) })

        bottomButtonsController = BottomButtonsController(ui, lifecycleScope)

        synchronizeTileScrolling()
    }

    private fun updateListType(listType: ListType) {
        when (listType) {
            ListType.Text -> {
                ui.utxosTextList.visible()
                ui.tileContainer.gone()
                ui.utxosTileLeftList.gone()
                ui.utxosTileRightList.gone()
            }
            ListType.Tile -> {
                ui.utxosTextList.gone()
                ui.tileContainer.visible()
                ui.utxosTileLeftList.visible()
                ui.utxosTileRightList.visible()
            }
        }
    }

    private fun updateState(screenState: ScreenState) {
        when (screenState) {
            ScreenState.Loading -> {
                ui.emptyContainer.setVisible(false)
                ui.loadingContainer.setVisible(true)
                ui.dataContainer.setVisible(false)
            }
            ScreenState.Empty -> {
                ui.emptyContainer.setVisible(true)
                ui.loadingContainer.setVisible(false)
                ui.dataContainer.setVisible(false)
            }
            ScreenState.Data -> {
                ui.emptyContainer.setVisible(false)
                ui.loadingContainer.setVisible(false)
                ui.dataContainer.setVisible(true)
            }
        }
    }

    private fun synchronizeTileScrolling() {
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