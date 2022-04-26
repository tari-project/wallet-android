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
package com.tari.android.wallet.ui.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.animation.addListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.databinding.ActivityAuthBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService.BiometricAuthenticationException
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.error.WalletErrorArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Initial activity class - authenticates the user.
 *
 * @author The Tari Development Team
 */
internal class AuthActivity : AppCompatActivity() {

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var resourceManager: ResourceManager

    private var authWasSuccessful = false
    private var isPending = true

    private lateinit var ui: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityAuthBinding.inflate(layoutInflater).apply { setContentView(root) }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        EventBus.walletState.subscribe(this, this::onWalletStateChanged)
        setupUi()
        walletServiceLauncher.start()
        if (savedInstanceState == null) {
            tracker.screen(path = "/local_auth", title = "Local Authentication")
        }
    }

    private fun setupUi() {
        ui.progressBar.setColor(color(white))
        ui.progressBar.invisible()
        // call the animations
        showTariText()
        val versionInfo = "${networkRepository.currentNetwork!!.network.displayName} ${BuildConfig.VERSION_NAME} b${BuildConfig.VERSION_CODE}"
        ui.networkInfoTextView.text = versionInfo
    }

    override fun onDestroy() {
        EventBus.walletState.unsubscribe(this)
        super.onDestroy()
    }

    override fun onBackPressed() = Unit

    private fun onWalletStateChanged(walletState: WalletState?) {
        if (authWasSuccessful && isPending) {
            if (walletState == WalletState.Running) {
                isPending = false
                EventBus.unsubscribeAll(this)
                ui.rootView.post(this::continueToHomeActivity)
            } else if (walletState is WalletState.Failed) {
                isPending = false
                showWalletError(walletState)
            }
        }
    }

    private fun showWalletError(state: WalletState.Failed) {
        lifecycleScope.launch(Dispatchers.Main) {
            val args = WalletErrorArgs(resourceManager, state.exception) { finish() }
            ModularDialog(this@AuthActivity, args.getErrorArgs().getModular(resourceManager)).show()
        }
    }

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

    /**
     * Calls Android authentication helper - does biometric if exists, falls back
     * on passcode if not.
     */
    private fun doAuth() {
        // check whether there's at least screen lock
        if (authService.isDeviceSecured) {
            lifecycleScope.launch {
                try {
                    // prompt system authentication dialog
                    authService.authenticate(
                        this@AuthActivity,
                        title = string(auth_title),
                        subtitle =
                        if (authService.isBiometricAuthAvailable) string(auth_biometric_prompt)
                        else string(auth_device_lock_code_prompt)
                    )
                    authSuccessful()
                } catch (e: BiometricAuthenticationException) {
                    if (e.code != ERROR_USER_CANCELED && e.code != ERROR_CANCELED)
                        Logger.e("Other biometric error. Code: ${e.code}")
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
        sharedPrefsWrapper.isAuthenticated = true
        playTariWalletAnim()
    }

    /**
     * Auth has failed.
     */
    private fun authHasFailed() {
        Logger.e("Authentication other error.")
        displayAuthFailedDialog()
    }

    /**
     * Auth not available on device, i.e. lock screen is disabled
     */
    private fun displayAuthNotAvailableDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(getString(auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(getString(proceed)) { dialog, _ ->
                dialog.cancel()
                // user has chosen to proceed without authentication
                sharedPrefsWrapper.isAuthenticated = true
                playTariWalletAnim()
            }
            // negative button text and action
            .setNegativeButton(getString(exit)) { _, _ -> finish() }
        val dialog = dialogBuilder.create()
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
            authWasSuccessful = true
            ui.progressBar.alpha = 0f
            ui.progressBar.visible()
            ObjectAnimator.ofFloat(ui.progressBar, View.ALPHA, 0f, 1f).apply {
                duration = Constants.UI.mediumDurationMs
                start()
            }

            onWalletStateChanged(EventBus.walletState.publishSubject.value)
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