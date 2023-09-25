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
package com.tari.android.wallet.ui.fragment.auth

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.animation.addListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.string.auth_biometric_prompt
import com.tari.android.wallet.R.string.auth_device_lock_code_prompt
import com.tari.android.wallet.R.string.auth_failed_desc
import com.tari.android.wallet.R.string.auth_failed_title
import com.tari.android.wallet.R.string.auth_not_available_or_canceled_desc
import com.tari.android.wallet.R.string.auth_not_available_or_canceled_title
import com.tari.android.wallet.R.string.auth_title
import com.tari.android.wallet.R.string.exit
import com.tari.android.wallet.R.string.proceed
import com.tari.android.wallet.databinding.ActivityAuthBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationException
import com.tari.android.wallet.ui.common.CommonActivity
import com.tari.android.wallet.ui.extension.addAnimatorListener
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.setColor
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.ui.fragment.settings.allSettings.TariVersionModel
import com.tari.android.wallet.util.TariBuild.MOCKED
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Initial activity class - authenticates the user.
 *
 * @author The Tari Development Team
 */
class AuthActivity : CommonActivity<ActivityAuthBinding, AuthViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAuthBinding.inflate(layoutInflater).apply { setContentView(root) }

        val viewModel: AuthViewModel by viewModels()
        bindViewModel(viewModel)

        setupUi()

        observe(viewModel.goAuth) {
            // call the animations
            showTariText()
            viewModel.walletServiceLauncher.start()
        }
    }

    private fun setupUi() {
        ui.progressBar.setColor(viewModel.paletteManager.getWhite(this))
        ui.progressBar.invisible()
        ui.networkInfoTextView.text = TariVersionModel(viewModel.networkRepository).versionInfo
    }

    override fun onBackPressed() = Unit

    /**
     * Hides the gem and displays Tari logo.
     */
    private fun showTariText() {
        // hide features to be shown after animation
        ui.authAnimLottieAnimationView.alpha = 0f
        ui.networkInfoTextView.alpha = 0f
        ui.smallGemImageView.alpha = 0f

        // define animations
        val hideGemAnim = ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.bigGemImageView.alpha = alpha
            }
        }
        val showTariTextAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.authAnimLottieAnimationView.alpha = alpha
                ui.networkInfoTextView.alpha = alpha
                ui.smallGemImageView.alpha = alpha
            }
        }
        AnimatorSet().apply {
            startDelay = Constants.UI.shortDurationMs
            play(showTariTextAnim).after(hideGemAnim)
            duration = Constants.UI.shortDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN)
            addListener(onEnd = { doAuth() })
            start()
        }
    }

    private fun doAuth() {
        // check whether there's at least screen lock
        if (viewModel.authService.isDeviceSecured) {
            lifecycleScope.launch {
                try {
                    if (!MOCKED) {
                        // prompt system authentication dialog
                        viewModel.authService.authenticate(
                            this@AuthActivity,
                            title = string(auth_title),
                            subtitle =
                            if (viewModel.authService.isBiometricAuthAvailable) string(auth_biometric_prompt)
                            else string(auth_device_lock_code_prompt)
                        )
                    }
                    authSuccessful()
                } catch (e: BiometricAuthenticationException) {
                    authHasFailed()
                }
            }
        } else {
            // local authentication not available
            displayAuthNotAvailableDialog()
        }
    }

    /**
     * Auth was successful.
     */
    private fun authSuccessful() {
        viewModel.sharedPrefsWrapper.isAuthenticated = true
        playTariWalletAnim()
    }

    /**
     * Auth has failed.
     */
    private fun authHasFailed() {
        displayAuthFailedDialog()
    }

    /**
     * Auth not available on device, i.e. lock screen is disabled
     */
    private fun displayAuthNotAvailableDialog() {
        val dialog = AlertDialog.Builder(this)
            .setMessage(getString(auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(getString(proceed)) { dialog, _ ->
                dialog.cancel()
                // user has chosen to proceed without authentication
                viewModel.sharedPrefsWrapper.isAuthenticated = true
                playTariWalletAnim()
            }
            // negative button text and action
            .setNegativeButton(getString(exit)) { _, _ -> finish() }
            .create()
        dialog.setTitle(getString(auth_not_available_or_canceled_title))
        dialog.show()
    }

    private fun displayAuthFailedDialog() {
        val state = lifecycle.currentState
        if (state == Lifecycle.State.RESUMED || state == Lifecycle.State.STARTED) {
            AlertDialog.Builder(this)
                .setMessage(getString(auth_failed_desc))
                .setCancelable(false)
                .setNegativeButton(getString(exit)) { _, _ -> finish() }
                .run(AlertDialog.Builder::create)
                .apply { setTitle(string(auth_failed_title)) }
                .show()
        }
    }

    /**
     * Plays Tari Wallet text anim.
     */
    private fun playTariWalletAnim() {
        ui.authAnimLottieAnimationView.addAnimatorListener(onEnd = {
            ui.progressBar.alpha = 0f
            ui.progressBar.visible()
            ObjectAnimator.ofFloat(ui.progressBar, View.ALPHA, 0f, 1f).apply {
                duration = Constants.UI.mediumDurationMs
                start()
            }

            proceedLogin()
        })
        ui.authAnimLottieAnimationView.playAnimation()

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = Constants.UI.mediumDurationMs
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.smallGemImageView.alpha = alpha
                ui.networkInfoTextView.alpha = alpha
            }
            startDelay = Constants.UI.CreateWallet.introductionBottomViewsFadeOutDelay
            start()
        }
    }

    private fun proceedLogin() {
        lifecycleScope.launch(Dispatchers.Main) {
            continueToHomeActivity()
        }
    }

    private fun continueToHomeActivity() {
        ObjectAnimator.ofFloat(ui.progressBar, View.ALPHA, 1f, 0f).apply {
            duration = Constants.UI.shortDurationMs
            start()
        }

        // go to home activity
        Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            this@AuthActivity.intent.data?.let(::setData)
            startActivity(this)
        }
        finish()
    }

}