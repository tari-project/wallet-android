package com.tari.android.wallet.ui.screen.exchange.sendFunds

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class SendFundsFragment : CommonFragment<SendFundsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            SendFundsScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onCopyAmount = { viewModel.onCopyAmount() },
                onCopyAddress = { viewModel.onCopyAddress() },
                onCancelTransaction = { viewModel.onBackPressed() },
                onOpenTxDetails = { viewModel.onOpenTxDetails() },
                onRetry = { viewModel.onRetry() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: SendFundsViewModel by viewModels()
        bindViewModel(viewModel)
    }

    companion object {
        const val ARG_REQUEST = "ARG_REQUEST"
        const val ARG_TRANSACTION = "ARG_TRANSACTION"

        fun newInstance(request: ExchangeRequestData? = null, transaction: Exolix.Transaction? = null) = SendFundsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(ARG_REQUEST, request)
                putParcelable(ARG_TRANSACTION, transaction)
            }
        }
    }
}
