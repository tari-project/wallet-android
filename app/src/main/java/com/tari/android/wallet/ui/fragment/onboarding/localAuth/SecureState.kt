package com.tari.android.wallet.ui.fragment.onboarding.localAuth

data class SecureState(
    val biometricsAvailable: Boolean = true,
    val pinCodeSecured: Boolean = false,
    val biometricsSecured: Boolean = false,
)