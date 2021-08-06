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
package com.tari.android.wallet.ui.fragment.onboarding

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.dimen.auth_button_bottom_margin
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.databinding.FragmentLocalAuthBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService.BiometricAuthenticationException
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService.BiometricAuthenticationType.*
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants.UI.Auth
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * onBoarding flow : Local authentication prompt
 *
 * @author The Tari Development Team
 */
internal class LocalAuthFragment : Fragment() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var authService: BiometricAuthenticationService

    private var authType = NONE
    private var listener: Listener? = null
    private var continueIsPendingOnWalletState = false
    private lateinit var ui: FragmentLocalAuthBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentLocalAuthBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        setDeviceAuthType()
        setupUi()
        ui.rootView.doOnGlobalLayout(this::playStartUpAnim)
        if (savedInstanceState == null) {
            tracker.screen(
                path = "/onboarding/enable_local_auth",
                title = "Onboarding - Enable Local Authentication"
            )
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            listener = context
        } else {
            throw AssertionError("Activity must implement listener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun setDeviceAuthType() {
        authType = when {
            authService.isBiometricAuthAvailable -> BIOMETRIC
            authService.isDeviceSecured -> PIN
            else -> NONE
        }
    }

    private fun setupUi() {
        ui.progressBarContainerView.invisible()
        ui.progressBar.setColor(color(R.color.white))
        if (authType == BIOMETRIC) {
            //setup ui for biometric auth
            ui.authTypeImageView.setImageResource(R.drawable.fingerprint)
            ui.enableAuthButton.text = string(auth_prompt_button_touch_id_text)
        } else {
            //setup ui for device lock code auth
            ui.authTypeImageView.setImageResource(R.drawable.numpad)
            ui.enableAuthButton.text = string(auth_prompt_button_text)
        }
        ui.enableAuthButton.setOnClickListener { onEnableAuthButtonClick(it) }
    }

    private fun playStartUpAnim() {
        val offset =
            (ui.enableAuthButtonContainerView.height + dimenPx(auth_button_bottom_margin)).toFloat()
        val buttonContainerViewAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(ui.enableAuthButtonContainerView, View.TRANSLATION_Y, offset, 0f)

        val titleOffset = -(ui.promptTextView.height).toFloat()
        val titleTextAnim =
            ObjectAnimator.ofFloat(
                ui.promptTextView,
                View.TRANSLATION_Y,
                0f,
                titleOffset
            )

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.smallGemImageView.alpha = alpha
            ui.authTypeImageView.alpha = alpha
            ui.authDescTextView.alpha = alpha
            ui.enableAuthButtonContainerView.alpha = alpha
        }
        fadeInAnim.startDelay = Auth.viewFadeAnimDelayMs

        val anim = AnimatorSet()
        anim.playTogether(titleTextAnim, buttonContainerViewAnim, fadeInAnim)
        anim.interpolator = EasingInterpolator(Ease.QUINT_IN)
        anim.startDelay = Auth.localAuthAnimDurationMs
        anim.duration = Auth.localAuthAnimDurationMs
        anim.start()
    }

    private fun onEnableAuthButtonClick(view: View) {
        view.temporarilyDisableClick()
        ui.enableAuthButton.animateClick {
            if (authType == NONE) {
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
                val isSuccessful = authService.authenticate(
                    fragment = this@LocalAuthFragment,
                    title = string(onboarding_auth_title),
                    subtitle = if (authType == BIOMETRIC) string(onboarding_auth_biometric_prompt)
                    else string(onboarding_auth_device_lock_code_prompt)
                )
                if (isSuccessful) authSuccess() else authFailed()
            } catch (e: BiometricAuthenticationException) {
                if (e.code != BiometricPrompt.ERROR_USER_CANCELED && e.code != BiometricPrompt.ERROR_CANCELED)
                    Logger.e("Other auth error. Code: ${e.code}")
                authFailed()
            }
        }
    }

    /**
     * Auth has failed.
     */
    private fun authFailed() {
        // TODO decide what do if there's no auth at all.
        //  For now let's display an alert dialog indicating the error
        Logger.e("Authentication other error.")
        context?.let {
            AlertDialog.Builder(it)
                .setMessage(string(auth_failed_desc))
                .setCancelable(false)
                // negative button text and action
                .setNegativeButton(string(common_ok), null)
                .create()
                .apply { setTitle(string(auth_failed_title)) }
                .show()
        }
        ui.enableAuthButton.isEnabled = true
    }

    /**
     * Auth not available on device
     */
    private fun displayAuthNotAvailableDialog() {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setMessage(string(auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(string(proceed)) { dialog, _ ->
                // user has chosen to proceed without authentication
                sharedPrefsWrapper.isAuthenticated = true
                dialog.cancel()
                authSuccess()
            }
            .setNegativeButton(string(common_cancel), null)

        val alert = dialogBuilder.create()
        alert.setTitle(string(auth_not_available_or_canceled_title))
        alert.show()
    }

    private fun authSuccess() {
        // check if the wallet is ready & switch to wait mode if not & start listening
        if (EventBus.walletState.publishSubject.value != WalletState.RUNNING) {
            ui.progressBarContainerView.visible()
            ui.enableAuthButton.invisible()
            continueIsPendingOnWalletState = true
            EventBus.walletState.subscribe(this) { walletState ->
                onWalletStateChanged(walletState)
            }
        } else {
            sharedPrefsWrapper.isAuthenticated = true
            sharedPrefsWrapper.onboardingAuthSetupCompleted = true
            listener?.onAuthSuccess()
        }
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.RUNNING && continueIsPendingOnWalletState) {
            continueIsPendingOnWalletState = false
            ui.rootView.post { authSuccess() }
        }
    }

    interface Listener {
        fun onAuthSuccess()
    }
}
