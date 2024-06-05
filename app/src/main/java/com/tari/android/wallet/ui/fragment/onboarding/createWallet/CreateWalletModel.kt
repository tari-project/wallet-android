package com.tari.android.wallet.ui.fragment.onboarding.createWallet

object CreateWalletModel {
    sealed class Effect{
        data object StartCheckmarkAnimation : Effect()
    }
}