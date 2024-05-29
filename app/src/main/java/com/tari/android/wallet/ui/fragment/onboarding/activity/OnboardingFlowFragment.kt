package com.tari.android.wallet.ui.fragment.onboarding.activity

import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.common.CommonViewModel

abstract class OnboardingFlowFragment<Binding : ViewBinding, VM : CommonViewModel> : CommonFragment<Binding, VM>() {

    val onboardingListener: OnboardingFlowListener
        get() = requireActivity().safeCastTo<OnboardingFlowListener>() ?: error("Should implement be started from OnboardingActivity")
}