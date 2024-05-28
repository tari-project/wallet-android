package com.tari.android.wallet.ui.fragment.onboarding.activity

object OnboardingFlowModel {
    sealed class Effect{
        data object ResetFlow : Effect()
    }
}