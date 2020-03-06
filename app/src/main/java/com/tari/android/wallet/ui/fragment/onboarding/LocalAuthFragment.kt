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
import butterknife.BindDimen
import butterknife.BindString
import butterknife.BindView
import butterknife.OnClick
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.auth.AuthUtil
import com.tari.android.wallet.ui.component.CustomFontButton
import com.tari.android.wallet.ui.component.CustomFontTextView
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

    @BindView(R.id.local_auth_vw_root)
    lateinit var rootView: FrameLayout
    @BindView(R.id.local_auth_btn_enable_auth)
    lateinit var enableAuthButton: CustomFontButton
    @BindView(R.id.local_auth_img_auth)
    lateinit var authTypeImageView: ImageView
    @BindView(R.id.local_auth_title_container)
    lateinit var authTitleTextContainer: LinearLayout
    @BindView(R.id.local_auth_txt_auth_desc)
    lateinit var authDescTextView: CustomFontTextView
    @BindView(R.id.local_auth_img_small_gem)
    lateinit var smallGemImageView: ImageView
    @BindView(R.id.local_auth_vw_auth_success_anim_container)
    lateinit var touchIdPromptContainer: FrameLayout
    @BindView(R.id.local_auth_vw_auth_success_bg)
    lateinit var authVerifyPromptBg: View
    @BindView(R.id.local_auth_vw_auth_success)
    lateinit var authVerifyPrompt: RelativeLayout
    @BindView(R.id.local_auth_txt_title_auth_type)
    lateinit var titleAuthTypeTextView: TextView
    @BindView(R.id.local_auth_img_auth_success_auth_type_image)
    lateinit var authSuccessAuthTypeImageView: ImageView
    @BindView(R.id.local_auth_prompt_auth_type_title)
    lateinit var authSuccessAuthTypeTitleTextView: TextView

    @BindDimen(R.dimen.auth_button_bottom_margin)
    @JvmField
    var useAuthButtonBottomMargin = 0

    @BindString(R.string.auth_prompt_button_text)
    lateinit var buttonAuthFormat: String
    @BindString(R.string.auth_prompt_touch_id)
    lateinit var authTouchId: String
    @BindString(R.string.auth_prompt_pin)
    lateinit var authPin: String
    @BindString(R.string.auth_prompt_touch_id_desc)
    lateinit var authTouchIdDesc: String
    @BindString(R.string.auth_prompt_pin_desc)
    lateinit var authPinDesc: String

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var tracker: Tracker

    private var authType: AuthType = AuthType.None

    private var listener: Listener? = null

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
                AuthType.Biometric
            }
            AuthUtil.isDeviceSecured(context!!) -> {
                AuthType.Pin
            }
            else -> {
                AuthType.None
            }
        }
    }

    @OnClick(R.id.local_auth_btn_enable_auth)
    fun onEnableAuthButtonClick() {
        UiUtil.temporarilyDisableClick(enableAuthButton)
        val animatorSet = UiUtil.animateButtonClick(enableAuthButton)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if (authType == AuthType.None) {
                    displayAuthNotAvailableDialog()
                    return
                }
                startAuthVerifyAnim(true)
            }
        })
    }

    private fun setupUi() {
        if (authType == AuthType.Biometric) {
            //setup ui for fingerprint auth
            titleAuthTypeTextView.text = authTouchId
            enableAuthButton.text = String.format(buttonAuthFormat, authTouchId)
            authDescTextView.text = authTouchIdDesc
            authTypeImageView.setImageResource(R.drawable.fingerprint)
            authSuccessAuthTypeImageView.setImageResource(R.drawable.fingerprint)
            authSuccessAuthTypeTitleTextView.text = authTouchId
        } else {
            //setup ui for pin or password auth
            titleAuthTypeTextView.text = authPin
            enableAuthButton.text = String.format(buttonAuthFormat, authPin)
            authDescTextView.text = authPinDesc
            authTypeImageView.setImageResource(R.drawable.numpad)
            authSuccessAuthTypeImageView.setImageResource(R.drawable.numpad)
            authSuccessAuthTypeTitleTextView.text = authPin
        }
    }

    private fun playStartUpAnim() {
        val offset = -(enableAuthButton.height + useAuthButtonBottomMargin).toFloat()

        val buttonAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(enableAuthButton, View.TRANSLATION_Y, 0f, offset)

        val titleOffset = -(authTitleTextContainer.height).toFloat()
        val titleTextAnim =
            ObjectAnimator.ofFloat(
                authTitleTextContainer,
                View.TRANSLATION_Y,
                0f,
                titleOffset
            )

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authDescTextView.alpha = alpha
            enableAuthButton.alpha = alpha
            authTypeImageView.alpha = alpha
            smallGemImageView.alpha = alpha
        }
        fadeInAnim.startDelay = Auth.viewFadeAnimDelayMs

        val anim = AnimatorSet()
        anim.playTogether(buttonAnim, titleTextAnim, fadeInAnim)
        anim.startDelay = Auth.localAuthAnimDurationMs
        anim.duration = Auth.localAuthAnimDurationMs
        anim.start()
    }

    private fun startAuthVerifyAnim(showAuthVerifyView: Boolean) {
        touchIdPromptContainer.visibility = View.VISIBLE
        val touchIdBgFadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        touchIdBgFadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authVerifyPromptBg.alpha = alpha
        }
        touchIdBgFadeInAnim.duration = Auth.touchIdPromptDurationMs

        val touchIdPromptFadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        touchIdPromptFadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authVerifyPrompt.alpha = alpha
        }

        touchIdPromptFadeInAnim.startDelay = Auth.touchIdPromptFadeInAnimDelayDuration
        touchIdPromptFadeInAnim.duration = Auth.touchIdPromptDurationMs

        val touchIdPromptFadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        touchIdPromptFadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authVerifyPrompt.alpha = alpha
        }

        touchIdPromptFadeOutAnim.startDelay = Auth.touchIdPromptFadeOutAnimDelayDuration
        touchIdPromptFadeOutAnim.duration = Auth.touchIdPromptDurationMs


        val touchIdBgFadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        touchIdBgFadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authVerifyPromptBg.alpha = alpha
        }
        touchIdBgFadeOutAnim.duration = Auth.touchIdPromptDurationMs

        val fadeoutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeoutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authDescTextView.alpha = alpha
            enableAuthButton.alpha = alpha
            authTypeImageView.alpha = alpha
            authTitleTextContainer.alpha = alpha
            authTypeImageView.alpha = alpha
            smallGemImageView.alpha = alpha
        }

        fadeoutAnim.startDelay = Auth.viewFadeAnimDelayMs
        fadeoutAnim.duration = Auth.touchIdPromptDurationMs

        val animatorSet = AnimatorSet()
        if (showAuthVerifyView)
            animatorSet.playSequentially(touchIdBgFadeInAnim, touchIdPromptFadeInAnim)
        else {
            authVerifyPrompt.alpha = 1f
            authVerifyPromptBg.alpha = 1f
            val fadeOutViews = AnimatorSet()
            fadeOutViews.playTogether(touchIdBgFadeOutAnim, fadeoutAnim)
            animatorSet.playSequentially(touchIdPromptFadeOutAnim, fadeOutViews)
        }
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if (showAuthVerifyView) {
                    doAuth()
                } else {
                    authSuccess()
                }
            }
        })
        animatorSet.start()
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
                            Logger.e("Other biometric error. Code: %d", errorCode)
                            authFailed()
                        }
                    }
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startAuthVerifyAnim(false)
                }
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_title))
            .setSubtitle(getString(R.string.auth_subtitle))
            .setDescription(getString(R.string.auth_description))
            .setDeviceCredentialAllowed(true)
            .build()
        biometricPrompt.authenticate(promptInfo)

        hideAuthVerifyAnim()
    }

    /**
     * Hide
     */
    private fun hideAuthVerifyAnim() {
        val fadeoutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeoutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            authVerifyPrompt.alpha = alpha
            authVerifyPromptBg.alpha = alpha
        }

        fadeoutAnim.startDelay = Auth.viewFadeAnimDelayMs
        fadeoutAnim.duration = Auth.viewFadeoutAnimMs
        fadeoutAnim.start()
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
            .setNegativeButton(getString(R.string.auth_prompt_cancel), null)

        val alert = dialogBuilder.create()
        alert.setTitle(getString(R.string.auth_failed_title))
        alert.show()
    }

    /**
     * Auth not available on device
     */
    private fun displayAuthNotAvailableDialog() {
        val dialogBuilder = AlertDialog.Builder(context!!)
        dialogBuilder.setMessage(getString(R.string.auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.proceed)) { dialog, _ ->
                dialog.cancel()
                authSuccess()
            }
            .setNegativeButton(getString(R.string.auth_prompt_cancel), null)


        val alert = dialogBuilder.create()
        alert.setTitle(getString(R.string.auth_not_available_or_canceled_title))
        alert.show()
    }

    private fun authSuccess() {
        sharedPrefsWrapper.onboardingAuthSetupCompleted = true
        listener?.onAuthSuccess()
    }

    interface Listener {
        fun onAuthSuccess()
    }
}

enum class AuthType {
    Biometric,
    Pin,
    None
}