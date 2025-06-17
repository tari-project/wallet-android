package com.tari.android.wallet.ui.screen.tx.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.composeContent

class TxHistoryFragment : CommonFragment<TxHistoryViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            TxHistoryScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onTxClick = { viewModel.navigateToTxDetail(it.tx) },
                onSearchQueryChange = { viewModel.onQueryChange(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: TxHistoryViewModel by viewModels()
        bindViewModel(viewModel)
    }

    companion object {
        const val PARAMETER_CONTACT = "PARAMETER_CONTACT"

        fun newInstance(contact: Contact? = null): TxHistoryFragment = TxHistoryFragment().apply {
            arguments = Bundle().apply { putParcelable(PARAMETER_CONTACT, contact) }
        }
    }
}