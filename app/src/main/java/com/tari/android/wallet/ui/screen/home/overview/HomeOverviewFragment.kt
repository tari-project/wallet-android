package com.tari.android.wallet.ui.screen.home.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.qr.QrScannerActivity
import com.tari.android.wallet.util.extension.parcelable
import kotlin.getValue

class HomeOverviewFragment : CommonFragment<HomeOverviewViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner))
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                TariDesignSystem(viewModel.currentTheme) {
                    HomeOverviewScreen(
                        uiState = uiState,
                        onStartMiningClicked = { viewModel.onStartMiningClicked() },
                        onSendTariClicked = { viewModel.onSendTariClicked() },
                        onRequestTariClicked = { viewModel.onRequestTariClicked() },
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: HomeOverviewViewModel by viewModels()
        bindViewModel(viewModel)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleDeeplink(requireActivity(), qrDeepLink)
        }
    }
}