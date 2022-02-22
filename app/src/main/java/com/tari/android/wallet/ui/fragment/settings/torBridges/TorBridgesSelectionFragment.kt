package com.tari.android.wallet.ui.fragment.settings.torBridges

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.databinding.FragmentTorBridgeSelectionBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.fragment.settings.allSettings.AllSettingsRouter
import com.tari.android.wallet.ui.fragment.settings.torBridges.torItem.TorBridgesAdapter

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

    private fun setupViews() = with(ui) {
        backCtaView.setOnThrottledClickListener { requireActivity().onBackPressed() }
        torBridgesList.layoutManager = LinearLayoutManager(requireContext())
        torBridgesList.adapter = adapter
        adapter.setClickListener(CommonAdapter.ItemClickListener { viewModel.select(it) })
    }

    private fun observeUI() = with(viewModel) {
        observe(torBridges) { adapter.update(it) }

        observe(navigation) { processNavigation(it) }
    }

    private fun processNavigation(navigation: TorBridgeNavigation) {
        val router = requireActivity() as AllSettingsRouter
        when (navigation) {
            TorBridgeNavigation.ToCustomBridges -> router.toCustomTorBridges()
        }
    }
}