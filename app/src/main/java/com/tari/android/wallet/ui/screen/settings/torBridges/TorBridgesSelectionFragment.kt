package com.tari.android.wallet.ui.screen.settings.torBridges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentTorBridgeSelectionBinding
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.screen.settings.torBridges.torItem.TorBridgesAdapter

class TorBridgesSelectionFragment : CommonFragment<FragmentTorBridgeSelectionBinding, TorBridgesSelectionViewModel>() {

    private var adapter: TorBridgesAdapter = TorBridgesAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentTorBridgeSelectionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: TorBridgesSelectionViewModel by viewModels()
        bindViewModel(viewModel)

        setupViews()

        observeUI()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    private fun setupViews() = with(ui) {
        torBridgesList.layoutManager = LinearLayoutManager(requireContext())
        torBridgesList.adapter = adapter
        adapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.preselect(it) })
        val actionArgs = TariToolbarActionArg(title = requireContext().getString(R.string.tor_bridges_connect)) {
            viewModel.connect()
        }
        adapter.setLongClickListener(CommonAdapter.ItemLongClickListener {
            viewModel.showBridgeQrCode(it)
            true
        })
        toolbar.setRightArgs(actionArgs)
    }

    private fun observeUI() = with(viewModel) {
        observe(torBridges) { adapter.update(it) }
    }
}