package com.tari.android.wallet.ui.screen.send.confirm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class ConfirmFragment : CommonFragment<ConfirmViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ConfirmScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onCopyValueClick = { viewModel.copyTxValueToClipboard(it) },
                onConfirmClick = { viewModel.onConfirmClicked() },
                onFeeInfoClick = { viewModel.onFeeInfoClicked() },
                onEmojiIdDetailsClick = { viewModel.onAddressDetailsClicked() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ConfirmViewModel by viewModels()
        bindViewModel(viewModel)
    }


    companion object {
        const val PARAMETER_TRANSACTION = "PARAMETER_TRANSACTION"

        fun newInstance(transactionData: TransactionData) = ConfirmFragment().apply {
            arguments = Bundle().apply {
                putParcelable(PARAMETER_TRANSACTION, transactionData)
            }
        }
    }
}