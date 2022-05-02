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
package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import android.animation.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.postDelayed
import androidx.fragment.app.viewModels
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.color.black
import com.tari.android.wallet.R.color.light_gray
import com.tari.android.wallet.R.dimen.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.databinding.FragmentCreateWalletBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateEmojiId
import com.tari.android.wallet.util.EmojiUtil
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

/**
 * onBoarding flow : wallet creation step.
 *
 * @author The Tari Development Team
 */
class CreateWalletFragment : CommonFragment<FragmentCreateWalletBinding, CreateWalletViewModel>() {

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController

    private val uiHandler = Handler(Looper.getMainLooper())

    private var emojiIdContinueButtonHasBeenDisplayed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentCreateWalletBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: CreateWalletViewModel by viewModels()
        bindViewModel(viewModel)

        setupUi()
        if (savedInstanceState == null) {
            viewModel.tracker.screen(path = "/onboarding/create_wallet", title = "Onboarding - Create Wallet")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun setupUi() {
        OverScrollDecoratorHelper.setUpOverScroll(ui.emojiIdScrollView)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView.root)
        ui.apply {
            yourEmojiIdTitleTextView.text = string(create_wallet_your_emoji_id_text_label).applyFontStyle(
                requireActivity(),
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
                createEmojiIdButton.setBottomMargin(createEmojiIdButton.height * -2)
                continueButton.setBottomMargin(continueButton.height * -2)
            }
            continueButton.setOnClickListener { onContinueButtonClick() }
            createEmojiIdButton.setOnClickListener { onCreateEmojiIdButtonClick() }
            emojiIdTextView.setOnClickListener { fullEmojiIdTextViewClicked(it) }
            arrayOf(seeFullEmojiIdButton, emojiIdSummaryContainerView)
                .forEach { it.setOnClickListener(this@CreateWalletFragment::onSeeFullEmojiIdButtonClicked) }
        }
    }

    private fun playStartupWhiteBgAnimation() {
        ObjectAnimator.ofFloat(ui.whiteBgView, View.TRANSLATION_Y, -ui.whiteBgView.height.toFloat(), 0f).apply {
            duration = CreateEmojiId.whiteBgAnimDurationMs
            interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
            addListener(object : AnimatorListenerAdapter() {
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
            start()
        }
    }

    private fun showBottomSpinner() {
        ObjectAnimator.ofFloat(ui.bottomSpinnerLottieAnimationView, "alpha", 0f, 1f).run {
            duration = Constants.UI.longDurationMs
            start()
        }
    }

    private fun showSecondViewByAnim() {
        val offset = -ui.justSecTitleTextView.height.toFloat()
        val titleAnim = ObjectAnimator.ofFloat(ui.justSecTitleTextView, View.TRANSLATION_Y, 0f, offset).apply {
            interpolator = EasingInterpolator(Ease.QUINT_OUT)
            startDelay = CreateEmojiId.titleShortAnimDelayMs
        }

        val descAnim = ObjectAnimator.ofFloat(ui.justSecDescTextView, View.TRANSLATION_Y, 0f, offset).apply {
            interpolator = EasingInterpolator(Ease.QUINT_OUT)
        }

        AnimatorSet().apply {
            playTogether(titleAnim, descAnim)
            duration = CreateEmojiId.helloTextAnimDurationMs
            interpolator = EasingInterpolator(Ease.QUART_OUT)
            addListener(object : AnimatorListenerAdapter() {
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
                        EventBus.walletState.publishSubject.filter { it == WalletState.Running }
                            .subscribe { startCheckMarkAnimation() }
                            .addTo(viewModel.compositeDisposable)
                    }
                }
            })
            start()
        }
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

        ObjectAnimator.ofFloat(ui.bottomSpinnerLottieAnimationView, "alpha", 1f, 0f).run {
            duration = CreateEmojiId.shortAlphaAnimDuration
            start()
        }

        ValueAnimator.ofFloat(1f, 0f).apply {
            duration = CreateEmojiId.shortAlphaAnimDuration
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.justSecDescTextView.alpha = alpha
                ui.justSecTitleTextView.alpha = alpha
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    val emojiId = viewModel.sharedPrefsWrapper.emojiId!!
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
            start()
        }
    }

    private fun startCreateEmojiIdAnimation() {
        // animation is looping, so we have to skip the fade-in and scale-up anims
        // that happen at the beginning of the animation
        ui.nerdFaceEmojiLottieAnimationView.setMinFrame(50)
        ui.nerdFaceEmojiLottieAnimationView.translationY = -(ui.nerdFaceEmojiLottieAnimationView.height).toFloat()
        ui.nerdFaceEmojiLottieAnimationView.playAnimation()

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.walletAddressDescTextView.alpha = alpha
                ui.createEmojiIdButton.alpha = alpha
                ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
                ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
            }
        }

        val createNowAnim: ObjectAnimator = ObjectAnimator.ofFloat(
            ui.createYourEmojiIdLine2TextView,
            View.TRANSLATION_Y,
            0f,
            -ui.createYourEmojiIdLine2TextView.height.toFloat()
        ).apply {
            duration = CreateEmojiId.awesomeTextAnimDurationMs
        }

        val awesomeAnim: ObjectAnimator = ObjectAnimator.ofFloat(
            ui.createYourEmojiIdLine1TextView,
            View.TRANSLATION_Y,
            0f,
            -ui.createYourEmojiIdLine1TextView.height.toFloat()
        ).apply {
            duration = CreateEmojiId.awesomeTextAnimDurationMs
        }

        val buttonInitialBottomMargin = ui.createEmojiIdButton.getBottomMargin()
        val buttonBottomMarginDelta = dimenPx(create_wallet_button_bottom_margin) - buttonInitialBottomMargin
        val buttonTranslationAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.createEmojiIdButton.setBottomMargin(
                    (buttonInitialBottomMargin + buttonBottomMarginDelta * value).toInt()
                )
            }
            duration = CreateEmojiId.awesomeTextAnimDurationMs
            startDelay = CreateEmojiId.createEmojiButtonAnimDelayMs
        }

        AnimatorSet().apply {
            playTogether(fadeInAnim, createNowAnim, awesomeAnim, buttonTranslationAnim)

            startDelay = CreateEmojiId.viewOverlapDelayMs
            duration = CreateEmojiId.createEmojiViewAnimDurationMs
            interpolator = EasingInterpolator(Ease.QUINT_OUT)
            addListener(object : AnimatorListenerAdapter() {
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
            start()
        }
    }

    private fun onCreateEmojiIdButtonClick() {
        ui.createEmojiIdButton.temporarilyDisableClick()
        ui.createEmojiIdButton.animateClick { showEmojiWheelAnimation() }
    }

    private fun showEmojiWheelAnimation() {
        viewModel.tracker.screen(path = "/onboarding/create_emoji_id", title = "Onboarding - Create Emoji Id")

        ui.emojiWheelLottieAnimationView.playAnimation()

        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.nerdFaceEmojiLottieAnimationView.alpha = alpha
                ui.createYourEmojiIdLine1TextView.alpha = alpha
                ui.createYourEmojiIdLine2TextView.alpha = alpha
                ui.walletAddressDescTextView.alpha = alpha
                ui.createEmojiIdButton.alpha = alpha
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    super.onAnimationEnd(animation)
                    ui.createEmojiIdButton.gone()
                    ui.createYourEmojiIdLine1BlockerView.gone()
                    ui.justSecTitleBackView.gone()
                    ui.justSecDescBackView.gone()
                    ui.createYourEmojiIdLine2BlockerView.gone()
                }
            })

            startDelay = CreateEmojiId.walletCreationFadeOutAnimDelayMs
            duration = CreateEmojiId.walletCreationFadeOutAnimDurationMs
            start()
        }

        uiHandler.postDelayed(
            { startYourEmojiIdViewAnimation() },
            ui.emojiWheelLottieAnimationView.duration - CreateEmojiId.awesomeTextAnimDurationMs
        )
    }

    private fun startYourEmojiIdViewAnimation() {
        val buttonFadeInAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.continueButton.alpha = alpha
            }
            duration = CreateEmojiId.continueButtonAnimDurationMs
        }

        ui.emojiIdTextView.isEnabled = false
        ui.emojiIdScrollView.scrollTo(ui.emojiIdTextView.width - ui.emojiIdScrollView.width, 0)
        val emojiIdContainerViewScaleAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { animation ->
                val value = animation.animatedValue.toString().toFloat()
                val scale = 1.0f + (1f - value) * 0.5f
                ui.emojiIdContainerView.scaleX = scale
                ui.emojiIdContainerView.scaleY = scale
            }
            startDelay = CreateEmojiId.emojiIdImageViewAnimDelayMs
        }

        val titleOffset = -(ui.yourEmojiIdTitleContainerView.height).toFloat()
        val yourEmojiTitleAnim = ObjectAnimator.ofFloat(ui.yourEmojiIdTitleContainerView, View.TRANSLATION_Y, 0f, titleOffset).apply {
            startDelay = CreateEmojiId.yourEmojiIdTextAnimDelayMs
            duration = CreateEmojiId.yourEmojiIdTextAnimDurationMs
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator?) {
                    super.onAnimationStart(animation)
                    ui.yourEmojiTitleBackView.visible()
                    ui.yourEmojiIdTitleContainerView.visible()
                }
            })
        }

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.emojiIdContainerView.alpha = alpha
                ui.emojiIdDescTextView.alpha = alpha
            }
            startDelay = CreateEmojiId.emojiIdImageViewAnimDelayMs
            duration = CreateEmojiId.continueButtonAnimDurationMs
        }


        AnimatorSet().apply {
            playTogether(
                buttonFadeInAnim,
                emojiIdContainerViewScaleAnim,
                fadeInAnim,
                yourEmojiTitleAnim
            )
            duration = CreateEmojiId.emojiIdCreationViewAnimDurationMs
            interpolator = EasingInterpolator(Ease.QUINT_IN)

            ui.seeFullEmojiIdContainerView.alpha = 0f
            ui.seeFullEmojiIdContainerView.visible()
            ui.emojiIdContainerView.visible()
            start()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    elevateEmojiIdContainerView()
                    ui.emojiIdScrollView.smoothScrollTo(0, 0)
                }
            })
        }

    }

    private fun elevateEmojiIdContainerView() {
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.emojiIdContainerView.elevation = value * dimen(common_view_elevation)
            }
            duration = Constants.UI.mediumDurationMs
            interpolator = EasingInterpolator(Ease.BACK_OUT)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    ui.seeFullEmojiIdButton.isEnabled = true
                    uiHandler.postDelayed({
                        hideFullEmojiId()
                    }, Constants.UI.mediumDurationMs)
                }
            })
            start()
        }
    }

    private fun showEmojiIdContinueButton() {
        val buttonInitialBottomMargin = ui.continueButton.getBottomMargin()
        val buttonBottomMarginDelta = dimenPx(create_wallet_button_bottom_margin) - buttonInitialBottomMargin
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.continueButton.setBottomMargin((buttonInitialBottomMargin + buttonBottomMarginDelta * value).toInt())
            }
            duration = CreateEmojiId.continueButtonAnimDurationMs
            start()
        }
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
        val fullEmojiIdDeltaWidth = (ui.rootView.width - dimenPx(common_horizontal_margin) * 2) - fullEmojiIdInitialWidth
        ui.emojiIdContainerView.setLayoutWidth(fullEmojiIdInitialWidth)
        ui.emojiIdContainerView.alpha = 0f
        ui.emojiIdContainerView.visible()
        ui.emojiIdTextView.isEnabled = true
        // scroll to end
        ui.emojiIdScrollView.post { ui.emojiIdScrollView.scrollTo(ui.emojiIdTextView.width - ui.emojiIdScrollView.width, 0) }
        // animate full emoji id view
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                // container alpha & scale
                ui.emojiIdContainerView.alpha = value
                ui.emojiIdSummaryContainerView.alpha = 1f - value
                val width = (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
                ui.emojiIdContainerView.setLayoutWidth(width)
                val margin = (dimenPx(onboarding_see_full_emoji_id_button_visible_top_margin) * (1f - value)).toInt()
                ui.seeFullEmojiIdContainerView.setTopMargin(margin)
                ui.seeFullEmojiIdContainerView.alpha = 1f - value
            }
            duration = Constants.UI.shortDurationMs
            start()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    ui.seeFullEmojiIdContainerView.invisible()
                    ui.emojiIdTextView.isEnabled = true
                    ui.emojiIdSummaryContainerView.invisible()
                    ui.emojiIdScrollView.postDelayed({ ui.emojiIdScrollView.smoothScrollTo(0, 0) }, Constants.UI.shortDurationMs + 20)
                }
            })
        }

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
        ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                val width = (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
                ui.emojiIdContainerView.setLayoutWidth(width)
                ui.emojiIdContainerView.alpha = (1f - value)
                ui.emojiIdSummaryContainerView.alpha = value
                ui.seeFullEmojiIdContainerView.setTopMargin((dimenPx(onboarding_see_full_emoji_id_button_visible_top_margin) * value).toInt())
                ui.seeFullEmojiIdContainerView.alpha = value
            }
            duration = Constants.UI.shortDurationMs
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    ui.emojiIdContainerView.invisible()
                }
            })
            start()
        }
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
        viewModel.sharedPrefsWrapper.onboardingCompleted = true
        ui.continueButton.animateClick {
            viewModel.sharedPrefsWrapper.onboardingAuthSetupStarted = true
            (requireActivity() as? CreateWalletListener)?.continueToEnableAuth()
        }
    }

    fun fadeOutAllViewAnimation() {
        val emojiIdViewToFadeOut = when (ui.emojiIdContainerView.visibility) {
            View.VISIBLE -> ui.emojiIdContainerView
            else -> ui.emojiIdSummaryContainerView
        }

        ValueAnimator.ofFloat(1f, 0f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val alpha = valueAnimator.animatedValue as Float
                ui.continueButton.alpha = alpha
                ui.emojiIdDescTextView.alpha = alpha
                ui.seeFullEmojiIdContainerView.alpha = alpha
                emojiIdViewToFadeOut.alpha = alpha

                ui.yourEmojiIdTitleContainerView.alpha = alpha
            }
            duration = Constants.UI.mediumDurationMs
            start()
        }
    }
}

