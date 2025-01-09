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
package com.tari.android.wallet.ui.screen.onboarding.inroduction

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.animation.addListener
import androidx.fragment.app.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.create_wallet_privacy_policy
import com.tari.android.wallet.R.string.create_wallet_user_agreement
import com.tari.android.wallet.R.string.create_wallet_user_agreement_and_privacy_policy
import com.tari.android.wallet.R.string.introduction_selected_wallet
import com.tari.android.wallet.R.string.privacy_policy_url
import com.tari.android.wallet.R.string.user_agreement_url
import com.tari.android.wallet.databinding.FragmentIntroductionBinding
import com.tari.android.wallet.ui.screen.onboarding.activity.OnboardingFlowFragment
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.extension.addAnimatorListener
import com.tari.android.wallet.util.extension.animateClick
import com.tari.android.wallet.util.extension.applyURLStyle
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.doOnGlobalLayout
import com.tari.android.wallet.util.extension.getResourceUri
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.invisible
import com.tari.android.wallet.util.extension.postDelayed
import com.tari.android.wallet.util.extension.setLayoutSize
import com.tari.android.wallet.util.extension.setOnThrottledClickListener
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.util.extension.temporarilyDisableClick
import com.tari.android.wallet.util.extension.visible
import kotlin.math.min

/**
 * onBoarding flow : wallet introduction screen.
 * Private key and wallet do not get created in this fragment.
 * They get created in the next fragment (CreateWalletFragment) after the user clicks the CTA.
 *
 * @author The Tari Development Team
 */

class IntroductionFragment : OnboardingFlowFragment<FragmentIntroductionBinding, IntroductionViewModel>() {

    private val handler = Handler(Looper.getMainLooper())

    private var videoViewHasBeenSetup = false
    private var videoViewLastPosition = 0

    private val createWalletArtificialDelay = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentIntroductionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: IntroductionViewModel by viewModels()
        bindViewModel(viewModel)

        setupUi()
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    override fun onStart() {
        super.onStart()
        if (videoViewHasBeenSetup) {
            startVideo()
        }
    }

    override fun onResume() {
        super.onResume()
        ui.selectNetworkButton.text = string(introduction_selected_wallet, viewModel.uiState.value.networkName)
    }

    override fun onPause() {
        videoViewLastPosition = ui.rainAnimationVideoView.currentPosition
        ui.rainAnimationVideoView.stopPlayback()
        super.onPause()
    }

    private fun setupUi() {
        ui.createWalletProgressBar.setWhite()
        collectFlow(viewModel.uiState) { uiState ->
            ui.apply {
                tariLogoLottieAnimationView.alpha = 0f
                tariLogoLottieAnimationView.scaleX = 0.84f
                tariLogoLottieAnimationView.scaleY = 0.84f
                networkInfoTextView.alpha = 0f
                smallGemImageView.alpha = 0f
                createWalletContainerView.alpha = 0f
                selectNetworkContainerView.alpha = 0f
                headerLineTopTextView.alpha = 0f
                headerLineBottomTextView.alpha = 0f
                userAgreementAndPrivacyPolicyTextView.alpha = 0f
                restoreWalletCtaView.alpha = 0f
                ui.restoreWalletCtaView.setOnClickListener {
                    viewModel.toWalletRestoreActivity()
                }
                networkInfoTextView.text = uiState.versionInfo
                // highlight links
                userAgreementAndPrivacyPolicyTextView.text =
                    SpannableString(string(create_wallet_user_agreement_and_privacy_policy)).apply {
                        applyURLStyle(string(create_wallet_user_agreement), string(user_agreement_url))
                        applyURLStyle(string(create_wallet_privacy_policy), string(privacy_policy_url))
                    }
                // make the links clickable
                userAgreementAndPrivacyPolicyTextView.movementMethod = LinkMovementMethod.getInstance()
                rootView.doOnGlobalLayout {
                    runStartupAnimation()
                    setupAndStartVideo()
                }
                createWalletButton.setOnThrottledClickListener { onCreateWalletClick() }
                selectNetworkContainerView.setOnThrottledClickListener { onboardingListener.navigateToNetworkSelection() }
            }
        }

    }

    private fun setupAndStartVideo() {
        val size = min(ui.videoOuterContainerView.width, ui.videoOuterContainerView.height)
        ui.videoInnerContainerView.setLayoutSize(size, size)
        ui.rainAnimationVideoView.setVideoURI(requireContext().getResourceUri(R.raw.purple_orb))
        ui.rainAnimationVideoView.setAudioFocusRequest(AudioManager.AUDIOFOCUS_NONE)
        startVideo()
        videoViewHasBeenSetup = true
    }

    private fun startVideo() {
        ui.rainAnimationVideoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            ui.rainAnimationVideoView.start()
            ui.rainAnimationVideoView.seekTo(videoViewLastPosition)
        }
    }

    private fun animateHeaderLineTextView(textView: TextView, startDelay: Long) {
        val height = textView.measuredHeight.toFloat()
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                textView.alpha = value
                textView.translationY = (1 - value) * height
            }
            this.startDelay = startDelay
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.SINE_OUT)
            start()
        }
    }

    private fun runStartupAnimation() {
        animateHeaderLineTextView(ui.headerLineTopTextView, Constants.UI.mediumDurationMs)
        animateHeaderLineTextView(ui.headerLineBottomTextView, Constants.UI.mediumDurationMs + Constants.UI.xShortDurationMs * 2)

        animations += ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.tariLogoLottieAnimationView.alpha = value
                ui.networkInfoTextView.alpha = value
                ui.smallGemImageView.alpha = value
                ui.createWalletContainerView.alpha = value
                ui.selectNetworkContainerView.alpha = value
                ui.userAgreementAndPrivacyPolicyTextView.alpha = value
                ui.restoreWalletCtaView.alpha = value
            }
            startDelay = Constants.UI.mediumDurationMs
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.SINE_OUT)
            start()
        }
    }

    private fun onCreateWalletClick() = with(ui) {
        createWalletButton.temporarilyDisableClick()
        restoreWalletCtaView.setOnClickListener(null)
        createWalletButton.gone()
        createWalletProgressBar.visible()
        viewModel.onCreateWalletClick()
        createWalletContainerView.animateClick {
            selectNetworkContainerView.isEnabled = false
            rootView.postDelayed(createWalletArtificialDelay) { startTariWalletViewAnimation() }
        }
    }

    private fun startTariWalletViewAnimation() {
        if (!isAdded) return // fragment might be detached from activity

        val metrics = resources.displayMetrics
        val offset = (metrics.heightPixels / 2 - ui.tariLogoLottieAnimationView.height / 2 - ui.tariLogoLottieAnimationView.top).toFloat()
        val tariViewTranslateAnim = ObjectAnimator.ofFloat(ui.tariLogoLottieAnimationView, View.TRANSLATION_Y, offset).apply {
            interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
            duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs
            addListener(onEnd = { playTariWalletLottieAnimation() })
        }.also { animations.add(it) }

        ui.tariLogoLottieAnimationView.addAnimatorListener(onEnd = { onboardingListener.continueToCreateWallet() })

        val tariViewScaleAnim = ValueAnimator.ofFloat(ui.tariLogoLottieAnimationView.scaleX, 1f).apply {
            duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs
            addUpdateListener { valueAnimator: ValueAnimator ->
                val scale = valueAnimator.animatedValue as Float
                ui.tariLogoLottieAnimationView.scaleY = scale
                ui.tariLogoLottieAnimationView.scaleX = scale
            }
        }.also { animations.add(it) }

        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = Constants.UI.CreateWallet.viewContainerFadeOutDurationMs
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.restoreWalletCtaView.alpha = alpha
                ui.videoOuterContainerView.alpha = alpha
                ui.headerLineTopTextView.alpha = alpha
                ui.headerLineBottomTextView.alpha = alpha
                ui.selectNetworkContainerView.alpha = alpha
                ui.createWalletContainerView.alpha = alpha
                ui.userAgreementAndPrivacyPolicyTextView.alpha = alpha
            }
            addListener(onEnd = {
                ui.videoOuterContainerView.invisible()
                ui.headerLineTopTextView.invisible()
                ui.headerLineBottomTextView.invisible()
                ui.selectNetworkContainerView.invisible()
                ui.createWalletContainerView.invisible()
                ui.userAgreementAndPrivacyPolicyTextView.invisible()
            })
        }.also { animations.add(it) }

        animations += AnimatorSet().apply {
            playTogether(tariViewTranslateAnim, tariViewScaleAnim, fadeOutAnim)
            start()
        }
    }

    private fun playTariWalletLottieAnimation() {
        ui.tariLogoLottieAnimationView.playAnimation()
        animations += ValueAnimator.ofFloat(1f, 0f).apply {
            duration = Constants.UI.mediumDurationMs
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.smallGemImageView.alpha = alpha
                ui.networkInfoTextView.alpha = alpha
            }
            startDelay = Constants.UI.Auth.bottomViewsFadeOutDelay
            start()
        }
    }
}
