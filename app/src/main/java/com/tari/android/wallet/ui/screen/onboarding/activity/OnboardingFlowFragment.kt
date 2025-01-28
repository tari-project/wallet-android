package com.tari.android.wallet.ui.screen.onboarding.activity

import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.util.extension.safeCastTo

abstract class OnboardingFlowXmlFragment<Binding : ViewBinding, VM : CommonViewModel> : CommonXmlFragment<Binding, VM>() {

    val onboardingListener: OnboardingFlowListener
        get() = requireActivity().safeCastTo<OnboardingFlowListener>() ?: error("Should be started from OnboardingActivity")
}

abstract class OnboardingFlowFragment<VM : CommonViewModel> : CommonFragment<VM>() {

    val onboardingListener: OnboardingFlowListener
        get() = requireActivity().safeCastTo<OnboardingFlowListener>() ?: error("Should be started from OnboardingActivity")
}