package com.tari.android.wallet.ui.fragment.onboarding.localAuth

object LocalAuthModel {
    data class SecureState(
        val biometricsAvailable: Boolean = true,
        val pinCodeSecured: Boolean = false,
        val biometricsSecured: Boolean = false,
    )

    sealed interface Effect {
        data object OnAuthSuccess : Effect
    }
}