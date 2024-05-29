package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import com.tari.android.wallet.tor.TorProxyState

object CreateWalletModel {
    data class UiState(
        val torState: TorProxyState = TorProxyState.NotReady,
    )
}