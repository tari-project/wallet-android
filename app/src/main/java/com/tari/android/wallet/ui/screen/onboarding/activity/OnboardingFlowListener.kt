package com.tari.android.wallet.ui.screen.onboarding.activity

interface OnboardingFlowListener {
    fun continueToEnableAuth()
    fun continueToCreateWallet()
    fun onAuthSuccess()
    fun navigateToNetworkSelection()
    fun resetFlow()
}