package com.tari.android.wallet.ui.screen.exchange.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class ExchangeReviewFragment : CommonFragment<ExchangeReviewViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ExchangeReviewScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onCopyValueClick = { viewModel.copyValueToClipboard(it) },
                onConfirmClick = { viewModel.onConfirmClicked() },
                onRetry = { viewModel.onRetry() },
                onEmojiIdDetailsClick = { viewModel.onAddressDetailsClicked() },
                onFeeInfoClick = { viewModel.onFeeInfoClicked() },
                onCancelTransaction = { viewModel.cancelTransaction() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ExchangeReviewViewModel by viewModels()
        bindViewModel(viewModel)
    }

    companion object {
        const val ARG_REQUEST = "ARG_REQUEST"

        fun newInstance(request: ExchangeRequestData) = ExchangeReviewFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_REQUEST, request)
            }
        }
    }
}
