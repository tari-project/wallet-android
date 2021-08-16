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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.color.black
import com.tari.android.wallet.R.color.light_gray
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.databinding.FragmentCreateWalletBinding
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateEmojiId
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import javax.inject.Inject
import javax.inject.Named

/**
 * onBoarding flow : wallet creation step.
 *
 * @author The Tari Development Team
 */
internal class CreateWalletFragment : Fragment() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String

    @Inject
    lateinit var tracker: Tracker

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private val uiHandler = Handler(Looper.getMainLooper())
    private var isWaitingOnWalletState = false
    private var emojiIdContinueButtonHasBeenDisplayed = false

    private lateinit var ui: FragmentCreateWalletBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentCreateWalletBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appComponent.inject(this)
        setupUi()
        if (savedInstanceState == null) {
            tracker.screen(path = "/onboarding/create_wallet", title = "Onboarding - Create Wallet")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun onWalletStateChanged(walletState: WalletState) {
        if (walletState == WalletState.RUNNING && isWaitingOnWalletState) {
            isWaitingOnWalletState = false
            ui.rootView.post { startCheckMarkAnimation() }
        }
    }

    private fun setupUi() {
        val mActivity = activity ?: return
        OverScrollDecoratorHelper.setUpOverScroll(ui.emojiIdScrollView)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView.root)
        ui.apply {
            yourEmojiIdTitleTextView.text =
                string(create_wallet_your_emoji_id_text_label).applyFontStyle(
                    mActivity,
                    CustomFont.AVENIR_LT_STD_LIGHT,
                    listOf(string(create_wallet_your_emoji_id_text_label_bold_part)),
                    CustomFont.AVENIR_LT_STD_BLACK
                )
            bottomSpinnerLottieAnimationView.alpha = 0f
            seeFullEmojiIdContainerView.invisible()
            emojiIdSummaryContainerView.invisible()
            emojiIdContainerView.invisible()
            seeFullEmojiIdButton.isEnabled = false

            continueButton.alpha = 0f
            createEmojiIdButton.alpha = 0f
            rootView.doOnGlobalLayout {
                whiteBgView.translationY = -whiteBgView.height.toFloat()
                playStartupWhiteBgAnimation()
                createEmojiIdButton.setBottomMargin(
                    createEmojiIdButton.height * -2
                )
                continueButton.setBottomMargin(
                    continueButton.height * -2
                )
            }
            continueButton.setOnClickListener { onContinueButtonClick() }
            createEmojiIdButton.setOnClickListener { onCreateEmojiIdButtonClick() }
            emojiIdTextView.setOnClickListener { fullEmojiIdTextViewClicked(it) }
            arrayOf(seeFullEmojiIdButton, emojiIdSummaryContainerView)
                .forEach {
                    it.setOnClickListener(this@CreateWalletFragment::onSeeFullEmojiIdButtonClicked)
                }
        }
    }

    private fun playStartupWhiteBgAnimation() {
        val whiteBgViewAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                ui.whiteBgView,
                View.TRANSLATION_Y,
                -ui.whiteBgView.height.toFloat(),
                0f
            )
        whiteBgViewAnim.duration = CreateEmojiId.whiteBgAnimDurationMs
        whiteBgViewAnim.interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
        whiteBgViewAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                showBottomSpinner()
                ui.justSecDescBackView.visible()
                ui.justSecTitleBackView.visible()
                showSecondViewByAnim()
            }

            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                ui.smallGemImageView.visible()
                ui.whiteBgView.visible()
            }
        })
        whiteBgViewAnim.start()
    }

    private fun showBottomSpinner() {
        ObjectAnimator.ofFloat(
            ui.bottomSpinnerLottieAnimationView,
            "alpha",
            0f, 1f
        ).run {
            duration = Constants.UI.longDurationMs
            start()
        }
    }

    private fun showSecondViewByAnim() {
        val offset = -ui.justSecTitleTextView.height.toFloat()
        val titleAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(ui.justSecTitleTextView, View.TRANSLATION_Y, 0f, offset)
        titleAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)
        titleAnim.startDelay = CreateEmojiId.titleShortAnimDelayMs
        val descAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(ui.justSecDescTextView, View.TRANSLATION_Y, 0f, offset)
        descAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)

        val animSet = AnimatorSet()
        animSet.playTogether(titleAnim, descAnim)
        animSet.duration = CreateEmojiId.helloTextAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                ui.justSecDescTextView.visible()
                ui.justSecTitleTextView.visible()
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                // if the wallet is not ready wait until it gets ready,
                // otherwise display the checkmark anim & move on
                uiHandler.postDelayed(CreateEmojiId.viewChangeAnimDelayMs) {
                    if (EventBus.walletState.publishSubject.value != WalletState.RUNNING) {
                        isWaitingOnWalletState = true
                        EventBus.walletState.subscribe(this, ::onWalletStateChanged)
                    } else {
                        startCheckMarkAnimation()
                    }
                }
            }
        })
        animSet.start()
    }

    private fun startCheckMarkAnimation() {
        ui.justSecDescBackView.gone()
        ui.justSecTitleBackView.gone()

        ui.checkmarkLottieAnimationView.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                startCreateEmojiIdAnimation()
            }
        })

        ObjectAnimator.ofFloat(
            ui.bottomSpinnerLottieAnimationView,
            "alpha",
            1f, 0f
        ).run {
            duration = CreateEmojiId.shortAlphaAnimDuration
            start()
        }

        val fadeOut = ValueAnimator.ofFloat(1f, 0f)
        fadeOut.duration = CreateEmojiId.shortAlphaAnimDuration
        fadeOut.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.justSecDescTextView.alpha = alpha
            ui.justSecTitleTextView.alpha = alpha
        }

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                val emojiId = sharedPrefsWrapper.emojiId!!
                ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
                    emojiId,
                    string(emoji_id_chunk_separator),
                    color(black),
                    color(light_gray)
                )
                emojiIdSummaryController.display(emojiId)

                ui.checkmarkLottieAnimationView.visible()
                ui.checkmarkLottieAnimationView.playAnimation()
            }
        })
        fadeOut.start()
    }

    private fun startCreateEmojiIdAnimation() {
        // animation is looping, so we have to skip the fade-in and scale-up anims
        // that happen at the beginning of the animation
        ui.nerdFaceEmojiLottieAnimationView.setMinFrame(50)
        ui.nerdFaceEmojiLottieAnimationView.translationY =
            -(ui.nerdFaceEmojiLottieAnimationView.height).toFloat()
        ui.nerdFaceEmojiLottieAnimationView.playAnimation()

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.walletAddressDescTextView.alpha = alpha
            ui.createEmojiIdButton.alpha = alpha
            ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
            ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
        }

        val createNowAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                ui.createYourEmojiIdLine2TextView,
                View.TRANSLATION_Y,
                0f,
                -ui.createYourEmojiIdLine2TextView.height.toFloat()
            )
        createNowAnim.duration = CreateEmojiId.awesomeTextAnimDurationMs

        val awesomeAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                ui.createYourEmojiIdLine1TextView,
                View.TRANSLATION_Y,
                0f,
                -ui.createYourEmojiIdLine1TextView.height.toFloat()
            )
        awesomeAnim.duration = CreateEmojiId.awesomeTextAnimDurationMs

        val buttonInitialBottomMargin = ui.createEmojiIdButton.getBottomMargin()
        val buttonBottomMarginDelta =
            dimenPx(create_wallet_button_bottom_margin) - buttonInitialBottomMargin
        val buttonTranslationAnim = ValueAnimator.ofFloat(0f, 1f)
        buttonTranslationAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.createEmojiIdButton.setBottomMargin(
                (buttonInitialBottomMargin + buttonBottomMarginDelta * value).toInt()
            )
        }
        buttonTranslationAnim.duration = CreateEmojiId.awesomeTextAnimDurationMs
        buttonTranslationAnim.startDelay = CreateEmojiId.createEmojiButtonAnimDelayMs

        val animSet = AnimatorSet()
        animSet.playTogether(fadeInAnim, createNowAnim, awesomeAnim, buttonTranslationAnim)

        animSet.startDelay = CreateEmojiId.viewOverlapDelayMs
        animSet.duration = CreateEmojiId.createEmojiViewAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUINT_OUT)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                ui.createYourEmojiIdLine2BlockerView.visible()
                ui.createYourEmojiIdLine1BlockerView.visible()
                ui.createYourEmojiIdLine1TextView.visible()
                ui.createYourEmojiIdLine2TextView.visible()
                ui.justSecDescBackView.gone()
                ui.justSecTitleBackView.gone()
            }
        })
        animSet.start()
    }

    private fun onCreateEmojiIdButtonClick() {
        ui.createEmojiIdButton.temporarilyDisableClick()
        ui.createEmojiIdButton.animateClick { showEmojiWheelAnimation() }
    }

    private fun showEmojiWheelAnimation() {
        tracker.screen(path = "/onboarding/create_emoji_id", title = "Onboarding - Create Emoji Id")

        ui.emojiWheelLottieAnimationView.playAnimation()

        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
            ui.createYourEmojiIdLine1TextView.alpha = alpha
            ui.createYourEmojiIdLine2TextView.alpha = alpha
            ui.walletAddressDescTextView.alpha = alpha
            ui.createEmojiIdButton.alpha = alpha
        }
        fadeOutAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                ui.createEmojiIdButton.gone()
                ui.createYourEmojiIdLine1BlockerView.gone()
                ui.justSecTitleBackView.gone()
                ui.justSecDescBackView.gone()
                ui.createYourEmojiIdLine2BlockerView.gone()
            }
        })

        fadeOutAnim.startDelay = CreateEmojiId.walletCreationFadeOutAnimDelayMs
        fadeOutAnim.duration = CreateEmojiId.walletCreationFadeOutAnimDurationMs
        fadeOutAnim.start()

        uiHandler.postDelayed(
            { startYourEmojiIdViewAnimation() },
            ui.emojiWheelLottieAnimationView.duration
                    - CreateEmojiId.awesomeTextAnimDurationMs
        )
    }

    private fun startYourEmojiIdViewAnimation() {
        val buttonFadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        buttonFadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.continueButton.alpha = alpha
        }
        buttonFadeInAnim.duration = CreateEmojiId.continueButtonAnimDurationMs

        ui.emojiIdTextView.isEnabled = false
        ui.emojiIdScrollView.scrollTo(
            ui.emojiIdTextView.width - ui.emojiIdScrollView.width,
            0
        )
        val emojiIdContainerViewScaleAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdContainerViewScaleAnim.addUpdateListener { animation ->
            val value = animation.animatedValue.toString().toFloat()
            val scale = 1.0f + (1f - value) * 0.5f
            ui.emojiIdContainerView.scaleX = scale
            ui.emojiIdContainerView.scaleY = scale
        }
        emojiIdContainerViewScaleAnim.startDelay = CreateEmojiId.emojiIdImageViewAnimDelayMs

        val titleOffset = -(ui.yourEmojiIdTitleContainerView.height).toFloat()
        val yourEmojiTitleAnim =
            ObjectAnimator.ofFloat(
                ui.yourEmojiIdTitleContainerView,
                View.TRANSLATION_Y,
                0f,
                titleOffset
            )
        yourEmojiTitleAnim.startDelay = CreateEmojiId.yourEmojiIdTextAnimDelayMs
        yourEmojiTitleAnim.duration = CreateEmojiId.yourEmojiIdTextAnimDurationMs
        yourEmojiTitleAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                ui.yourEmojiTitleBackView.visible()
                ui.yourEmojiIdTitleContainerView.visible()
            }
        })

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.emojiIdContainerView.alpha = alpha
            ui.emojiIdDescTextView.alpha = alpha
        }
        fadeInAnim.startDelay = CreateEmojiId.emojiIdImageViewAnimDelayMs
        fadeInAnim.duration = CreateEmojiId.continueButtonAnimDurationMs

        val animSet = AnimatorSet()
        animSet.playTogether(
            buttonFadeInAnim,
            emojiIdContainerViewScaleAnim,
            fadeInAnim,
            yourEmojiTitleAnim
        )
        animSet.duration = CreateEmojiId.emojiIdCreationViewAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUINT_IN)

        ui.seeFullEmojiIdContainerView.alpha = 0f
        ui.seeFullEmojiIdContainerView.visible()
        ui.emojiIdContainerView.visible()
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                elevateEmojiIdContainerView()
                ui.emojiIdScrollView.smoothScrollTo(0, 0)
            }
        })
    }

    private fun elevateEmojiIdContainerView() {
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.emojiIdContainerView.elevation = value * dimen(common_view_elevation)
        }
        anim.duration = Constants.UI.mediumDurationMs
        anim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        anim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                ui.seeFullEmojiIdButton.isEnabled = true
                uiHandler.postDelayed({
                    hideFullEmojiId()
                }, Constants.UI.mediumDurationMs)
            }
        })
        anim.start()
    }

    private fun showEmojiIdContinueButton() {
        val buttonInitialBottomMargin = ui.continueButton.getBottomMargin()
        val buttonBottomMarginDelta =
            dimenPx(create_wallet_button_bottom_margin) - buttonInitialBottomMargin
        val buttonTranslationAnim = ValueAnimator.ofFloat(0f, 1f)
        buttonTranslationAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.continueButton.setBottomMargin(
                (buttonInitialBottomMargin + buttonBottomMarginDelta * value).toInt()
            )
        }
        buttonTranslationAnim.duration = CreateEmojiId.continueButtonAnimDurationMs
        buttonTranslationAnim.start()
    }

    private fun onSeeFullEmojiIdButtonClicked(view: View) {
        view.temporarilyDisableClick()
        showFullEmojiId()
        if (!emojiIdContinueButtonHasBeenDisplayed) {
            showEmojiIdContinueButton()
            emojiIdContinueButtonHasBeenDisplayed = true
        }
    }

    /**
     * Maximize the emoji id view.
     */
    private fun showFullEmojiId() {
        // prepare views
        val fullEmojiIdInitialWidth = ui.emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth =
            (ui.rootView.width - dimenPx(common_horizontal_margin) * 2) - fullEmojiIdInitialWidth
        ui.emojiIdContainerView.setLayoutWidth(fullEmojiIdInitialWidth)
        ui.emojiIdContainerView.alpha = 0f
        ui.emojiIdContainerView.visible()
        ui.emojiIdTextView.isEnabled = true
        // scroll to end
        ui.emojiIdScrollView.post {
            ui.emojiIdScrollView.scrollTo(
                ui.emojiIdTextView.width - ui.emojiIdScrollView.width,
                0
            )
        }
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // container alpha & scale
            ui.emojiIdContainerView.alpha = value
            ui.emojiIdSummaryContainerView.alpha = 1f - value
            val width = (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            ui.emojiIdContainerView.setLayoutWidth(width)
            val margin =
                (dimenPx(onboarding_see_full_emoji_id_button_visible_top_margin) * (1f - value)).toInt()
            ui.seeFullEmojiIdContainerView.setTopMargin(margin)
            ui.seeFullEmojiIdContainerView.alpha = 1f - value
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        emojiIdAnim.start()
        emojiIdAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                ui.seeFullEmojiIdContainerView.invisible()
                ui.emojiIdTextView.isEnabled = true
                ui.emojiIdSummaryContainerView.invisible()
                ui.emojiIdScrollView.postDelayed({
                    ui.emojiIdScrollView.smoothScrollTo(0, 0)
                }, Constants.UI.shortDurationMs + 20)
            }
        })
    }

    /**
     * Minimizes full (expanded) emoji id view.
     */
    private fun hideFullEmojiId() {
        ui.emojiIdTextView.isEnabled = false
        ui.emojiIdSummaryContainerView.visible()
        ui.emojiIdSummaryContainerView.alpha = 0f
        ui.emojiIdSummaryContainerView.elevation = dimen(common_view_elevation)
        ui.seeFullEmojiIdContainerView.visible()
        ui.emojiIdScrollView.smoothScrollTo(0, 0)

        val fullEmojiIdInitialWidth = ui.emojiIdContainerView.width
        val fullEmojiIdDeltaWidth = ui.emojiIdSummaryContainerView.width - fullEmojiIdInitialWidth
        // animate full emoji id view
        val emojiIdWidthAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdWidthAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            val width = (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            ui.emojiIdContainerView.setLayoutWidth(width)
            ui.emojiIdContainerView.alpha = (1f - value)
            ui.emojiIdSummaryContainerView.alpha = value
            ui.seeFullEmojiIdContainerView.setTopMargin(
                (dimenPx(onboarding_see_full_emoji_id_button_visible_top_margin) * value).toInt()
            )
            ui.seeFullEmojiIdContainerView.alpha = value
        }
        emojiIdWidthAnim.duration = Constants.UI.shortDurationMs
        emojiIdWidthAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                ui.emojiIdContainerView.invisible()
            }
        })
        emojiIdWidthAnim.start()
    }

    /**
     * Minimize the emoji id view.
     */
    private fun fullEmojiIdTextViewClicked(view: View) {
        view.temporarilyDisableClick()
        hideFullEmojiId()
    }

    private fun onContinueButtonClick() {
        ui.continueButton.temporarilyDisableClick()
        sharedPrefsWrapper.onboardingCompleted = true
        ui.continueButton.animateClick {
            sharedPrefsWrapper.onboardingAuthSetupStarted = true
            (requireActivity() as Listener).continueToEnableAuth()
        }
    }

    fun fadeOutAllViewAnimation() {
        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        val emojiIdViewToFadeOut = when (ui.emojiIdContainerView.visibility) {
            View.VISIBLE -> ui.emojiIdContainerView
            else -> ui.emojiIdSummaryContainerView
        }
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            ui.continueButton.alpha = alpha
            ui.emojiIdDescTextView.alpha = alpha
            ui.seeFullEmojiIdContainerView.alpha = alpha
            emojiIdViewToFadeOut.alpha = alpha

            ui.yourEmojiIdTitleContainerView.alpha = alpha
        }
        fadeOutAnim.duration = Constants.UI.mediumDurationMs
        fadeOutAnim.start()
    }

    interface Listener {
        fun continueToEnableAuth()
    }

}
