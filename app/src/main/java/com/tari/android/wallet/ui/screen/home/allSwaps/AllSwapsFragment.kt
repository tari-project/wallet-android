package com.tari.android.wallet.ui.screen.home.allSwaps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class AllSwapsFragment : CommonFragment<AllSwapsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            AllSwapsScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onPullToRefresh = { viewModel.loadSwaps() },
                onRetry = { viewModel.loadSwaps() },
                onSwapClick = viewModel::onSwapClick,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AllSwapsViewModel by viewModels()
        bindViewModel(viewModel)
    }

    companion object {
        fun newInstance() = AllSwapsFragment()
    }
}
