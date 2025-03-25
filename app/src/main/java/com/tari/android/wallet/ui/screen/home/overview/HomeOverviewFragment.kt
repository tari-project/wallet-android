package com.tari.android.wallet.ui.screen.home.overview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.qr.QrScannerActivity
import com.tari.android.wallet.util.extension.composeContent
import com.tari.android.wallet.util.extension.parcelable

class HomeOverviewFragment : CommonFragment<HomeOverviewViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            HomeOverviewScreen(
                uiState = uiState,
                onInviteFriendClick = { viewModel.onInviteFriendClicked() },
                onNotificationsClick = { viewModel.onNotificationsClicked() },
                onStartMiningClicked = { viewModel.onStartMiningClicked() },
                onSendTariClicked = { viewModel.onSendTariClicked() },
                onRequestTariClicked = { viewModel.onRequestTariClicked() },
                onTxClick = { viewModel.navigateToTxDetail(it.tx) },
                onViewAllTxsClick = { viewModel.onAllTxClicked() },
                onConnectionStatusClick = { viewModel.showConnectionStatusDialog() },
                onSyncDialogDismiss = { viewModel.onSyncDialogDismiss() },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: HomeOverviewViewModel by viewModels()
        bindViewModel(viewModel)

        viewModel.checkPermission()
    }

    override fun onResume() {
        super.onResume()
        viewModel.grantContactsPermission()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleDeeplink(requireActivity(), qrDeepLink)
        }
    }
}