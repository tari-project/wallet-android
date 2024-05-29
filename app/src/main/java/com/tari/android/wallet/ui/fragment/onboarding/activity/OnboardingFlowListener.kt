package com.tari.android.wallet.ui.fragment.onboarding.activity

interface OnboardingFlowListener {
    fun continueToEnableAuth()
    fun continueToCreateWallet()
    fun onAuthSuccess()
    fun navigateToNetworkSelection()
}