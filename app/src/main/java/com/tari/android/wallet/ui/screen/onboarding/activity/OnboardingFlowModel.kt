package com.tari.android.wallet.ui.screen.onboarding.activity

object OnboardingFlowModel {
    sealed class Effect{
        data object ResetFlow : Effect()
    }
}