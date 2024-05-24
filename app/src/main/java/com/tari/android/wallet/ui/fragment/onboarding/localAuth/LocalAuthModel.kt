package com.tari.android.wallet.ui.fragment.onboarding.localAuth

object LocalAuthModel {

    sealed interface Effect {
        data object OnAuthSuccess : Effect
    }

    interface LocalAuthListener {
        fun onAuthSuccess()
    }
}