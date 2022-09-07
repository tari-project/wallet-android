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
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.R.dimen.auth_button_bottom_margin
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.databinding.FragmentLocalAuthBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationException
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationType
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants.UI.Auth
import kotlinx.coroutines.launch

class LocalAuthFragment : CommonFragment<FragmentLocalAuthBinding, LocalAuthViewModel>() {

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
        observeOnLoad(authType)
    }

    private fun setupUi() = with(ui) {
        progressBarContainerView.invisible()
        progressBar.setColor(color(R.color.white))
        if (viewModel.authType.value == BiometricAuthenticationType.BIOMETRIC) {
            //setup ui for biometric auth
            authTypeImageView.setImageResource(R.drawable.fingerprint)
            enableAuthButton.text = string(auth_prompt_button_touch_id_text)
        } else {
            //setup ui for device lock code auth
            authTypeImageView.setImageResource(R.drawable.numpad)
            enableAuthButton.text = string(auth_prompt_button_text)
        }
        enableAuthButton.setOnClickListener { onEnableAuthButtonClick(it) }
    }

    private fun playStartUpAnim() {
        val offset = (ui.enableAuthButtonContainerView.height + dimenPx(auth_button_bottom_margin)).toFloat()
        val buttonContainerViewAnim = ObjectAnimator.ofFloat(ui.enableAuthButtonContainerView, View.TRANSLATION_Y, offset, 0f)

        val titleOffset = -(ui.promptTextView.height).toFloat()
        val titleTextAnim = ObjectAnimator.ofFloat(ui.promptTextView, View.TRANSLATION_Y, 0f, titleOffset)

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.smallGemImageView.alpha = alpha
                ui.authTypeImageView.alpha = alpha
                ui.authDescTextView.alpha = alpha
                ui.enableAuthButtonContainerView.alpha = alpha
            }
            startDelay = Auth.viewFadeAnimDelayMs
        }

        AnimatorSet().apply {
            playTogether(titleTextAnim, buttonContainerViewAnim, fadeInAnim)
            interpolator = EasingInterpolator(Ease.QUINT_IN)
            startDelay = Auth.localAuthAnimDurationMs
            duration = Auth.localAuthAnimDurationMs
            start()
        }
    }

    private fun onEnableAuthButtonClick(view: View) {
        view.temporarilyDisableClick()
        ui.enableAuthButton.animateClick {
            if (viewModel.authType.value == BiometricAuthenticationType.NONE) {
                displayAuthNotAvailableDialog()
            } else {
                ui.enableAuthButton.isEnabled = false
                doAuth()
            }
        }
    }

    private fun doAuth() {
        lifecycleScope.launch {
            try {
                val subtitle = if (viewModel.authType.value == BiometricAuthenticationType.BIOMETRIC) string(onboarding_auth_biometric_prompt)
                else string(onboarding_auth_device_lock_code_prompt)
                if (viewModel.authService.authenticate(this@LocalAuthFragment, string(onboarding_auth_title), subtitle))
                    authSuccess() else authFailed()
            } catch (exception: BiometricAuthenticationException) {
                authFailed()
            }
        }
    }

    private fun authFailed() {
        AlertDialog.Builder(requireContext())
            .setMessage(string(auth_failed_desc))
            .setCancelable(false)
            .setNegativeButton(string(common_ok), null)
            .create()
            .apply { setTitle(string(auth_failed_title)) }
            .show()
        ui.enableAuthButton.isEnabled = true
    }

    private fun displayAuthNotAvailableDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage(string(auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(string(proceed)) { dialog, _ ->
                // user has chosen to proceed without authentication
                viewModel.sharedPrefsWrapper.isAuthenticated = true
                dialog.cancel()
                authSuccess()
            }
            .setNegativeButton(string(common_cancel), null)
            .create().apply {
                setTitle(string(auth_not_available_or_canceled_title))
                show()
            }
    }

    private fun authSuccess() {
        // check if the wallet is ready & switch to wait mode if not & start listening
        ui.progressBarContainerView.visible()
        ui.enableAuthButton.invisible()

        EventBus.walletState.publishSubject
            .filter { it == WalletState.Running }
            .subscribe {
                viewModel.sharedPrefsWrapper.isAuthenticated = true
                viewModel.sharedPrefsWrapper.onboardingAuthSetupCompleted = true
                (requireActivity() as? LocalAuthListener)?.onAuthSuccess()
            }.addTo(viewModel.compositeDisposable)
    }
}