package com.tari.android.wallet.ui.screen.onboarding.activity

import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.util.extension.safeCastTo

abstract class OnboardingFlowFragment<Binding : ViewBinding, VM : CommonViewModel> : CommonXmlFragment<Binding, VM>() {

    val onboardingListener: OnboardingFlowListener
        get() = requireActivity().safeCastTo<OnboardingFlowListener>() ?: error("Should implement be started from OnboardingActivity")
}