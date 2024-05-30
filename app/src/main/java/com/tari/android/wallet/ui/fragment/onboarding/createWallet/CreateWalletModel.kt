package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import com.tari.android.wallet.tor.TorProxyState

object CreateWalletModel {
    sealed class Effect{
        data object StartCheckmarkAnimation : Effect()
    }

    data class UiState(
        val torState: TorProxyState = TorProxyState.NotReady,
    )
}