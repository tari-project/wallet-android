package com.tari.android.wallet.ui.screen.tx.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsModel.SHOW_CLOSE_BUTTON_EXTRA_KEY
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsModel.TX_EXTRA_KEY
import com.tari.android.wallet.util.extension.composeContent

class TxDetailsFragment : CommonFragment<TxDetailsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            TxDetailsScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onCancelTxClick = { viewModel.onTransactionCancel() },
                onCopyValueClick = { viewModel.onCopyValueClicked(it) },
                onBlockExplorerClick = { viewModel.openInBlockExplorer() },
                onContactEditClick = { viewModel.onContactEditClicked() },
                onFeeInfoClick = { viewModel.onFeeInfoClicked() },
                onEmojiIdDetailsClick = { viewModel.onAddressDetailsClicked() },
                onRawDetailsClick = { viewModel.onRawDetailsClicked() },
                onPaymentReferenceInfoClick = { viewModel.onPaymentReferenceInfoClicked() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: TxDetailsViewModel by viewModels()
        bindViewModel(viewModel)
    }


    companion object {
        fun newInstance(tx: Tx, showCloseButton: Boolean = false) = TxDetailsFragment().apply {
            arguments = Bundle().apply {
                putParcelable(TX_EXTRA_KEY, tx)
                putBoolean(SHOW_CLOSE_BUTTON_EXTRA_KEY, showCloseButton)
            }
        }
    }
}