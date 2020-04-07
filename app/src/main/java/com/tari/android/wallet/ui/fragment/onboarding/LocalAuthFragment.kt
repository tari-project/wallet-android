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

import android.animation.*
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import butterknife.*
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.auth.AuthUtil
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.component.CustomFontButton
import com.tari.android.wallet.ui.component.CustomFontTextView
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants.UI.Auth
import com.tari.android.wallet.util.SharedPrefsWrapper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import javax.inject.Inject

/**
 * onBoarding flow : Local authentication prompt
 *
 * @author The Tari Development Team
 */
internal class LocalAuthFragment : BaseFragment() {

    enum class AuthType {
        BIOMETRIC,
        PIN,
        NONE
    }

    @BindView(R.id.local_auth_vw_root)
    lateinit var rootView: View
    @BindView(R.id.local_auth_img_small_gem)
    lateinit var smallGemImageView: ImageView
    @BindView(R.id.local_auth_img_auth)
    lateinit var authTypeImageView: ImageView
    @BindView(R.id.local_auth_prompt_text)
    lateinit var promptTextView: TextView
    @BindView(R.id.local_auth_txt_auth_desc)
    lateinit var authDescTextView: CustomFontTextView
    @BindView(R.id.local_auth_vw_enable_auth_button_container)
    lateinit var enableAuthButtonContainerView: View
    @BindView(R.id.local_auth_btn_enable_auth)
    lateinit var enableAuthButton: CustomFontButton
    @BindView(R.id.local_auth_vw_prog_bar_container)
    lateinit var progressBarContainerView: View
    @BindView(R.id.local_auth_prog_bar_enable_auth)
    lateinit var progressBar: ProgressBar

    @BindDimen(R.dimen.auth_button_bottom_margin)
    @JvmField
    var useAuthButtonBottomMargin = 0

    @BindString(R.string.auth_prompt_button_touch_id_text)
    lateinit var buttonTouchIdAuthFormat: String
    @BindString(R.string.auth_prompt_button_text)
    lateinit var buttonPinAuthFormat: String
    @BindString(R.string.onboarding_auth_biometric_prompt)
    lateinit var biometricAuthPrompt: String
    @BindString(R.string.onboarding_auth_device_lock_code_prompt)
    lateinit var deviceLockCodePrompt: String

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var tracker: Tracker

    private var authType: AuthType = AuthType.NONE
    private var listener: Listener? = null
    private var continueIsPendingOnWalletState = false

    override val contentViewId = R.layout.fragment_local_auth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setDeviceAuthType()
        setupUi()

        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    playStartUpAnim()
                }
            })

        TrackHelper.track()
            .screen("/onboarding/enable_local_auth")
            .title("Onboarding - Enable Local Authentication")
            .with(tracker)
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
            BiometricManager.from(context!!).canAuthenticate() == BIOMETRIC_SUCCESS -> {
                AuthType.BIOMETRIC
            }
            AuthUtil.isDeviceSecured(context!!) -> {
                AuthType.PIN
            }
            else -> {
                AuthType.NONE
            }
        }
    }

    private fun setupUi() {
        progressBarContainerView.invisible()
        UiUtil.setProgressBarColor(progressBar, whiteColor)
        if (authType == AuthType.BIOMETRIC) {
            //setup ui for biometric auth
            authTypeImageView.setImageResource(R.drawable.fingerprint)
            enableAuthButton.text = buttonTouchIdAuthFormat
        } else {
            //setup ui for device lock code auth
            authTypeImageView.setImageResource(R.drawable.numpad)
            enableAuthButton.text = buttonPinAuthFormat
        }
    }

    private fun playStartUpAnim() {
        val offset = (enableAuthButtonContainerView.height + useAuthButtonBottomMargin).toFloat()

        val buttonContainerViewAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(enableAuthButtonContainerView, View.TRANSLATION_Y, offset, 0f)

        val titleOffset = -(promptTextView.height).toFloat()
        val titleTextAnim =
            ObjectAnimator.ofFloat(
                promptTextView,
                View.TRANSLATION_Y,
                0f,
                titleOffset
            )

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            smallGemImageView.alpha = alpha
            authTypeImageView.alpha = alpha
            authDescTextView.alpha = alpha
            enableAuthButtonContainerView.alpha = alpha
        }
        fadeInAnim.startDelay = Auth.viewFadeAnimDelayMs

        val anim = AnimatorSet()
        anim.playTogether(titleTextAnim, buttonContainerViewAnim, fadeInAnim)
        anim.startDelay = Auth.localAuthAnimDurationMs
        anim.duration = Auth.localAuthAnimDurationMs
        anim.start()
    }

    @OnClick(R.id.local_auth_btn_enable_auth)
    fun onEnableAuthButtonClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        val animatorSet = UiUtil.animateButtonClick(enableAuthButton)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                if (authType == AuthType.NONE) {
                    displayAuthNotAvailableDialog()
                    return
                }
                enableAuthButton.isEnabled = false
                doAuth()
            }
        })
    }

    private fun doAuth() {
        // display authentication dialog
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)
                    when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED -> authFailed()
                        BiometricPrompt.ERROR_CANCELED -> authFailed()
                        else -> {
                            Logger.e("Other auth error. Code: %d", errorCode)
                            authFailed()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // startAuthVerifyAnim(false)
                    authSuccess()
                }
            })

        val biometricAuthAvailable = BiometricManager.from(context!!).canAuthenticate() == BIOMETRIC_SUCCESS
        val prompt = if (biometricAuthAvailable) biometricAuthPrompt else deviceLockCodePrompt
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.onboarding_auth_title))
            .setSubtitle(prompt)
            .setDeviceCredentialAllowed(true)
            .build()
        biometricPrompt.authenticate(promptInfo)
        // hideAuthVerifyAnim()
    }

    /**
     * Auth has failed.
     */
    private fun authFailed() {
        // TODO decide what do if there's no auth at all.
        //  For now let's display an alert dialog indicating the error
        Logger.e("Authentication other error.")

        val dialogBuilder = AlertDialog.Builder(context!!)
        dialogBuilder.setMessage(getString(R.string.auth_failed_desc))
            .setCancelable(false)
            // negative button text and action
            .setNegativeButton(getString(R.string.common_ok), null)

        val dialog = dialogBuilder.create()
        dialog.setTitle(getString(R.string.auth_failed_title))
        dialog.show()
        enableAuthButton.isEnabled = true
    }

    /**
     * Auth not available on device
     */
    private fun displayAuthNotAvailableDialog() {
        val dialogBuilder = AlertDialog.Builder(context!!)
        dialogBuilder.setMessage(getString(R.string.auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.proceed)) { dialog, _ ->
                // user has chosen to proceed without authentication
                sharedPrefsWrapper.isAuthenticated = true
                dialog.cancel()
                authSuccess()
            }
            .setNegativeButton(getString(R.string.common_cancel), null)

        val alert = dialogBuilder.create()
        alert.setTitle(getString(R.string.auth_not_available_or_canceled_title))
        alert.show()
    }

    private fun authSuccess() {
        // check if the wallet is ready & switch to wait mode if not & start listening
        if (EventBus.walletStateSubject.value != WalletState.RUNNING) {
            progressBarContainerView.visible()
            enableAuthButton.invisible()
            continueIsPendingOnWalletState = true
            EventBus.subscribeToWalletState(this) { walletState ->
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
            rootView.post {
                authSuccess()
            }
        }
    }

    interface Listener {
        fun onAuthSuccess()
    }
}