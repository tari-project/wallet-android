/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.onboarding.localAuth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.string.onboarding_auth_biometric_prompt
import com.tari.android.wallet.R.string.onboarding_auth_title
import com.tari.android.wallet.databinding.FragmentLocalAuthBinding
import com.tari.android.wallet.extension.launchAndRepeatOnLifecycle
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationException
import com.tari.android.wallet.ui.extension.doOnGlobalLayout
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.onboarding.activity.OnboardingFlowFragment
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthModel.Effect
import com.tari.android.wallet.util.Constants.UI.Auth
import kotlinx.coroutines.launch

class LocalAuthFragment : OnboardingFlowFragment<FragmentLocalAuthBinding, LocalAuthViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentLocalAuthBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: LocalAuthViewModel by viewModels()
        bindViewModel(viewModel)

        observeUi()
        setupUi()
        ui.rootView.doOnGlobalLayout(this::playStartUpAnim)
    }

    private fun observeUi() = with(viewModel) {
        viewLifecycleOwner.launchAndRepeatOnLifecycle(Lifecycle.State.STARTED) {
            launch {
                secureState.collect { state ->
                    ui.continueBtn.setVisible(state.pinCodeSecured)
                    ui.authTypeBiometrics.setVisible(state.biometricsAvailable)
                    ui.secureWithPasscode.setVisible(!state.pinCodeSecured)
                    ui.secureWithBiometrics.setVisible(state.biometricsAvailable && !state.biometricsSecured)
                }
            }

            launch {
                viewModel.effect.collect { effect ->
                    when (effect) {
                        is Effect.OnAuthSuccess -> {
                            onboardingListener.onAuthSuccess()
                        }
                    }
                }
            }
        }
    }

    private fun setupUi() {
        ui.continueBtn.setOnThrottledClickListener { viewModel.proceedToMain() }
        ui.secureWithBiometrics.setOnThrottledClickListener { doBiometricAuth() }
        ui.authTypeBiometrics.setOnThrottledClickListener { doBiometricAuth() }
        ui.authTypePasscode.setOnThrottledClickListener { viewModel.goToEnterPinCode() }
        ui.secureWithPasscode.setOnThrottledClickListener { viewModel.goToEnterPinCode() }
    }

    private fun playStartUpAnim() {
        val titleOffset = -(ui.promptTextView.height).toFloat()
        val titleTextAnim = ObjectAnimator.ofFloat(ui.promptTextView, View.TRANSLATION_Y, 0f, titleOffset)

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.smallGemImageView.alpha = alpha
                ui.authTypeBiometrics.alpha = alpha
                ui.authTypePasscode.alpha = alpha
                ui.authDescTextView.alpha = alpha
            }
            startDelay = Auth.viewFadeAnimDelayMs
        }

        AnimatorSet().apply {
            playTogether(titleTextAnim, fadeInAnim)
            interpolator = EasingInterpolator(Ease.QUINT_IN)
            startDelay = Auth.localAuthAnimDurationMs
            duration = Auth.localAuthAnimDurationMs
            start()
        }
    }

    private fun doBiometricAuth() {
        lifecycleScope.launch {
            try {
                val subtitle = string(onboarding_auth_biometric_prompt)
                if (viewModel.authService.authenticate(this@LocalAuthFragment, string(onboarding_auth_title), subtitle)) {
                    viewModel.securedWithBiometrics()
                }
            } catch (exception: BiometricAuthenticationException) {
                viewModel.logger.i(exception.message + "Biometric authentication failed")
            }
        }
    }
}