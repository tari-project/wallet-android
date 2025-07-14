package com.tari.android.wallet.ui.screen.contactBook.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.qr.QrScannerSource
import com.tari.android.wallet.util.extension.composeContent

class AddContactFragment : CommonFragment<AddContactViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val state by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            AddContactScreen(
                uiState = state,
                onBackClick = { viewModel.onBackPressed() },
                onScanQrClick = { startQrScanner(QrScannerSource.AddContact) },
                onSaveClick = { viewModel.saveContact() },
                onAliasChange = { viewModel.onAliasChange(it) },
                onAddressChange = { viewModel.onAddressChange(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AddContactViewModel by viewModels()
        bindViewModel(viewModel)
    }
}