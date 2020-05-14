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

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.white
import com.tari.android.wallet.R.string.auth_biometric_prompt
import com.tari.android.wallet.R.string.auth_device_lock_code_prompt
import com.tari.android.wallet.application.TariWalletApplication
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.auth.AuthUtil
import com.tari.android.wallet.databinding.ActivityAuthBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.service.WalletService
import com.tari.android.wallet.ui.activity.home.HomeActivity
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import java.lang.ref.WeakReference
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Initial activity class - authenticates the user.
 *
 * @author The Tari Development Team
 */
internal class AuthActivity : AppCompatActivity(), Animator.AnimatorListener {

    private lateinit var biometricPrompt: BiometricPrompt

    @Inject
    lateinit var tracker: Tracker

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    private var continueIsPendingOnWalletState = false

    private lateinit var ui: ActivityAuthBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAuthBinding.inflate(layoutInflater).apply { setContentView(root) }
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        AuthActivityVisitor.visit(this)
        EventBus.subscribeToWalletState(this, this::onWalletStateChanged)
        setupUi()
        tracker.screen(path = "/local_auth", title = "Local Authentication")
        startWalletService()
    }

    private fun startWalletService() {
        // start the wallet service
        ContextCompat.startForegroundService(
            applicationContext,
            Intent(applicationContext, WalletService::class.java)
        )
    }

    private fun setupUi() {
        UiUtil.setProgressBarColor(ui.progressBar, color(white))
        ui.progressBar.invisible()
        // call the animations
        val wr = WeakReference(this)
        ui.bigGemImageView.post { wr.get()?.showTariText() }
    }

    override fun onDestroy() {
        EventBus.unsubscribeFromWalletState(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        // no-op
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.RUNNING && continueIsPendingOnWalletState) {
            continueIsPendingOnWalletState = false
            ui.rootView.post(this::continueToHomeActivity)
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
        val hideGemAnim = ValueAnimator.ofFloat(1f, 0f)
        val showTariTextAnim = ValueAnimator.ofFloat(0f, 1f)
        val weakReference: WeakReference<AuthActivity> = WeakReference(this)
        hideGemAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            weakReference.get()?.ui?.bigGemImageView?.alpha = alpha
        }
        showTariTextAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            weakReference.get()?.ui?.let {
                val alpha = valueAnimator.animatedValue as Float
                it.authAnimLottieAnimationView.alpha = alpha
                it.networkInfoTextView.alpha = alpha
                it.smallGemImageView.alpha = alpha
            }
        }

        // chain animations
        val animSet = AnimatorSet()
        animSet.startDelay = Constants.UI.shortDurationMs
        animSet.play(showTariTextAnim).after(hideGemAnim)
        animSet.duration = Constants.UI.shortDurationMs
        // define interpolator
        animSet.interpolator = EasingInterpolator(Ease.QUART_IN)
        // authenticate at the end of the animation set
        val wr = WeakReference(this)
        animSet.addListener(onEnd = { wr.get()?.doAuth() })
        // start the animation set
        animSet.start()
    }

    /**
     * Calls Android authentication helper - does biometric if exists, falls back
     * on passcode if not.
     */
    private fun doAuth() {
        val wr = WeakReference(this)

        // check whether there's at least screen lock
        if (!AuthUtil.isDeviceSecured(this)) {
            // local authentication not available
            ui.authAnimLottieAnimationView.post {
                wr.get()?.displayAuthNotAvailableDialog()
            }
            return
        }

        // display authentication dialog
        val executor = Executors.newSingleThreadExecutor()
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> wr.get()?.authHasFailed()
                        BiometricPrompt.ERROR_CANCELED -> wr.get()?.authHasFailed()
                        else -> {
                            Logger.e("Other biometric error. Code: %d", errorCode)
                            wr.get()?.authHasFailed()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    wr.get()?.authSuccessful()
                }

            })

        val biometricAuthAvailable =
            BiometricManager.from(applicationContext)
                .canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS
        val prompt =
            if (biometricAuthAvailable) string(auth_biometric_prompt)
            else string(auth_device_lock_code_prompt)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_title))
            .setSubtitle(prompt)
            .setDeviceCredentialAllowed(true) // enable passcode (i.e. screenlock)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

    /**
     * Auth was successful.
     */
    private fun authSuccessful() {
        sharedPrefsWrapper.isAuthenticated = true
        val wr = WeakReference(this)
        wr.get()?.ui?.authAnimLottieAnimationView?.post {
            wr.get()?.playTariWalletAnim()
        }
    }

    /**
     * Auth has failed.
     */
    private fun authHasFailed() {
        Logger.e("Authentication other error.")
        val wr = WeakReference(this)
        runOnUiThread { wr.get()?.displayAuthFailedDialog() }
    }

    /**
     * Auth not available on device, i.e. lock screen is disabled
     */
    private fun displayAuthNotAvailableDialog() {
        val wr = WeakReference(this)
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(getString(R.string.auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.proceed)) { dialog, _ ->
                dialog.cancel()
                // user has chosen to proceed without authentication
                sharedPrefsWrapper.isAuthenticated = true
                wr.get()?.ui?.authAnimLottieAnimationView?.post {
                    wr.get()?.playTariWalletAnim()
                }
            }
            // negative button text and action
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                finish()
            }

        val dialog = dialogBuilder.create()
        dialog.setTitle(getString(R.string.auth_not_available_or_canceled_title))
        dialog.show()
    }

    private fun displayAuthFailedDialog() {
        biometricPrompt.cancelAuthentication()

        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setMessage(getString(R.string.auth_failed_desc))
            .setCancelable(false)
            // negative button text and action
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                finish()
            }

        val alert = dialogBuilder.create()
        alert.setTitle(getString(R.string.auth_failed_title))
        alert.show()
    }

    /**
     * Plays Tari Wallet text anim.
     */
    private fun playTariWalletAnim() {
        ui.authAnimLottieAnimationView.addAnimatorListener(this)
        ui.authAnimLottieAnimationView.playAnimation()

        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.duration = Constants.UI.mediumDurationMs
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.smallGemImageView.alpha = alpha
            ui.networkInfoTextView.alpha = alpha
        }
        fadeOutAnim.startDelay = Constants.UI.CreateWallet.introductionBottomViewsFadeOutDelay
        fadeOutAnim.start()
    }

    //region Animator Listener
    override fun onAnimationStart(animation: Animator?) {
        // no-op
    }

    override fun onAnimationRepeat(animation: Animator?) {
        // no-op
    }

    override fun onAnimationCancel(animation: Animator?) {
        // no-op
    }

    override fun onAnimationEnd(animation: Animator?) {
        if (EventBus.walletStateSubject.value != WalletState.RUNNING) {
            continueIsPendingOnWalletState = true
            ui.progressBar.alpha = 0f
            ui.progressBar.visible()
            val alphaAnim = ObjectAnimator.ofFloat(ui.progressBar, View.ALPHA, 0f, 1f)
            alphaAnim.duration = Constants.UI.mediumDurationMs
            alphaAnim.start()
        } else {
            continueToHomeActivity()
        }
    }
    //endregion Animator Listener

    private fun continueToHomeActivity() {
        val alphaAnim = ObjectAnimator.ofFloat(ui.progressBar, View.ALPHA, 1f, 0f)
        alphaAnim.duration = Constants.UI.shortDurationMs
        alphaAnim.start()
        // go to home activity
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        // finish this activity
        finish()
    }

    private object AuthActivityVisitor {

        fun visit(activity: AuthActivity) {
            (activity.application as TariWalletApplication).appComponent.inject(activity)
        }

    }

}
