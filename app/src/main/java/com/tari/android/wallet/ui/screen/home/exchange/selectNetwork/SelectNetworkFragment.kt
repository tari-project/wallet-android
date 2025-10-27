package com.tari.android.wallet.ui.screen.home.exchange.selectNetwork

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.composeContent

class SelectNetworkFragment : CommonFragment<SelectNetworkViewModel>() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val state by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            SelectNetworkScreen(
                uiState = state,
                onBackClick = { viewModel.onBackPressed() },
                onSearchQueryChange = { viewModel.onQueryChange(it) },
                onNetworkItemClick = { viewModel.onNetworkItemClicked(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: SelectNetworkViewModel by viewModels()
        bindViewModel(viewModel)

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                is SelectNetworkViewModel.Effect.SetSelectResult -> {
                    setFragmentResult(NETWORK_REQUEST_KEY, Bundle().apply { putParcelable(NETWORK_RESULT_KEY, effect.selectedNetwork) })
                }
            }
        }
    }

    companion object {
        const val NETWORK_REQUEST_KEY = "NETWORK_REQUEST_KEY"
        const val NETWORK_RESULT_KEY = "NETWORK_RESULT_KEY"

        const val NETWORKS_KEY = "NETWORKS_KEY"
        const val PRESELECTED_NETWORK_KEY = "PRESELECTED_NETWORK_KEY"

        fun newInstance(networks: List<Exolix.Network>, preselectedNetwork: Exolix.Network? = null) = SelectNetworkFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(NETWORKS_KEY, ArrayList(networks))
                putParcelable(PRESELECTED_NETWORK_KEY, preselectedNetwork)
            }
        }
    }
}