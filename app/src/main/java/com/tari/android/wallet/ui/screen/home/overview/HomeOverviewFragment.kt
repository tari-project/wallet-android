package com.tari.android.wallet.ui.screen.home.overview

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

class HomeOverviewFragment : CommonFragment<HomeOverviewViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            HomeOverviewScreen(
                uiState = uiState,
                onPullToRefresh = { viewModel.refreshData() },
                onStartMiningClicked = { viewModel.onStartMiningClicked() },
                onSendTariClicked = { viewModel.onSendTariClicked() },
                onRequestTariClicked = { viewModel.onRequestTariClicked() },
                onTxClick = { viewModel.navigateToTxDetail(it.tx) },
                onViewAllTxsClick = { viewModel.onAllTxClicked() },
                onConnectionStatusClick = { viewModel.showConnectionStatusDialog() },
                onConnectionStatusDismiss = { viewModel.onConnectionStatusDialogDismiss() },
                onSyncDialogDismiss = { viewModel.onSyncDialogDismiss() },
                onBalanceInfoClicked = { viewModel.onBalanceInfoClicked() },
                onBalanceInfoDialogDismiss = { viewModel.onBalanceInfoDialogDismiss() },
                onHideBalanceClicked = { viewModel.toggleBalanceHidden() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: HomeOverviewViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.checkPermission()
    }
}