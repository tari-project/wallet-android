package com.tari.android.wallet.ui.screen.onboarding.createWallet

object CreateWalletModel {
    sealed class Effect{
        data object StartCheckmarkAnimation : Effect()
    }
}