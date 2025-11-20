package com.tari.android.wallet.ui.screen.exchange.status

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

class ExchangeStatusFragment : CommonFragment<ExchangeStatusViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ExchangeStatusScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onCopyClick = viewModel::onCopyClicked,
                onRetry = viewModel::loadTransaction,
                onPullToRefresh = viewModel::onRefreshClicked,
                onShowQrCodeClick = viewModel::onShowQrCodeClicked,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ExchangeStatusViewModel by viewModels()
        bindViewModel(viewModel)
    }

    companion object {
        const val ARG_TRANSACTION_ID = "ARG_TRANSACTION_ID"

        fun newInstance(transactionId: String) = ExchangeStatusFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_TRANSACTION_ID, transactionId)
            }
        }
    }
}
