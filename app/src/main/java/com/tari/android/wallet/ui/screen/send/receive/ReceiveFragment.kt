package com.tari.android.wallet.ui.screen.send.receive

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

class ReceiveFragment : CommonFragment<ReceiveViewModel>() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val state by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            ReceiveScreen(
                uiState = state,
                onBackClick = { viewModel.onBackPressed() },
                onEmojiCopyClick = { viewModel.onEmojiCopyClick() },
                onBase58CopyClick = { viewModel.onBase58CopyClick() },
                onEmojiDetailClick = { viewModel.onAddressDetailsClicked() },
                onShareClick = { viewModel.onShareClick() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ReceiveViewModel by viewModels()
        bindViewModel(viewModel)
    }
}