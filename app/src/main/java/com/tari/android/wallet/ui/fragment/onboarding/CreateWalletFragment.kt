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
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import butterknife.*
import com.airbnb.lottie.LottieAnimationView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.di.ConfigModule
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.ffi.FFITestWallet
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateEmojiId
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.Long.max
import javax.inject.Inject
import javax.inject.Named

/**
 * onBoarding flow : wallet creation step.
 *
 * @author The Tari Development Team
 */
internal class CreateWalletFragment : BaseFragment() {

    @BindView(R.id.create_wallet_vw_root)
    lateinit var rootView: View
    @BindView(R.id.create_wallet_txt_hello)
    lateinit var helloText: TextView
    @BindView(R.id.create_wallet_txt_just_sec_desc)
    lateinit var justSecDescText: TextView
    @BindView(R.id.create_wallet_txt_just_sec_title)
    lateinit var justSecTitle: TextView
    @BindView(R.id.create_wallet_vw_hello_text_back)
    lateinit var helloTextBackView: View
    @BindView(R.id.create_wallet_checkmark_anim)
    lateinit var checkMarkAnim: LottieAnimationView
    @BindView(R.id.create_wallet_txt_wallet_address_desc)
    lateinit var walletAddressDescText: TextView
    @BindView(R.id.create_wallet_btn_create_emoji_id)
    lateinit var createEmojiIdButton: Button
    @BindView(R.id.create_wallet_just_sec_title_back_view)
    lateinit var justSecTitleBackView: View
    @BindView(R.id.create_wallet_just_sec_desc_back_view)
    lateinit var justSecDescBackView: View
    @BindView(R.id.create_wallet_nerd_face_emoji)
    lateinit var nerdFaceEmojiLottieAnim: LottieAnimationView
    @BindView(R.id.create_wallet_txt_create_your_emoji_id_line_2)
    lateinit var createYourEmojiIdLine2TextView: TextView
    @BindView(R.id.create_wallet_vw_create_your_emoji_id_line_2_blocker)
    lateinit var createEmojiIdTextBackView: View
    @BindView(R.id.create_wallet_txt_create_your_emoji_id_line_1)
    lateinit var createYourEmojiIdLine1TextView: TextView
    @BindView(R.id.create_wallet_white_bg_view)
    lateinit var whiteBgView: View
    @BindView(R.id.create_wallet_vw_create_your_emoji_id_line_1_blocker)
    lateinit var awesomeTextBackView: View
    @BindView(R.id.create_wallet_emoji_wheel_anim)
    lateinit var emojiWheelAnimView: LottieAnimationView
    @BindView(R.id.create_wallet_btn_see_full_emoji_id)
    lateinit var tapToSeeFullEmojiIdButton: Button
    @BindView(R.id.create_wallet_vw_emoji_id_summary_container)
    lateinit var emojiIdSummaryContainerView: View
    @BindView(R.id.create_wallet_vw_emoji_id_summary)
    lateinit var emojiIdSummaryView: View
    @BindView(R.id.create_wallet_vw_emoji_id_container)
    lateinit var emojiIdContainerView: View
    @BindView(R.id.create_wallet_scroll_emoji_id)
    lateinit var emojiIdScrollView: HorizontalScrollView
    @BindView(R.id.create_wallet_txt_emoji_id)
    lateinit var emojiIdTextView: TextView
    @BindView(R.id.create_wallet_vw_your_emoji_id_title_container)
    lateinit var yourEmojiIdTitleContainerView: LinearLayout
    @BindView(R.id.create_wallet_txt_your_emoji_id_title)
    lateinit var yourEmojiIdTitleTextView: TextView
    @BindView(R.id.create_wallet_your_emoji_title_back_view)
    lateinit var yourEmojiTitleBackView: View
    @BindView(R.id.create_wallet_txt_emoji_id_desc)
    lateinit var emojiIdDescriptionTextView: TextView
    @BindView(R.id.create_wallet_btn_continue)
    lateinit var continueButton: Button
    @BindView(R.id.create_wallet_anim_bottom_spinner)
    lateinit var bottomSpinnerLottieAnim: LottieAnimationView
    @BindView(R.id.create_wallet_img_small_gem)
    lateinit var smallGemImageView: ImageView
    @BindView(R.id.create_wallet_vw_see_full_emoji_id_container)
    lateinit var seeFullEmojiIdButtonContainerView: View

    @BindDimen(R.dimen.create_wallet_button_bottom_margin)
    @JvmField
    var createEmojiButtonBottomMargin = 0
    @BindDimen(R.dimen.common_view_elevation)
    @JvmField
    var emojiIdContainerElevation = 0
    @BindDimen(R.dimen.onboarding_see_full_emoji_id_button_visible_top_margin)
    @JvmField
    var seeFullEmojiIdButtonVisibleTopMargin = 0
    @BindDimen(R.dimen.common_horizontal_margin)
    @JvmField
    var horizontalMargin = 0

    @BindString(R.string.create_wallet_set_of_emoji_your_wallet_address_desc)
    @JvmField
    var yourWalletAddressDescString = ""
    /**
     * Emoji id chunk separator char.
     */
    @BindString(R.string.emoji_id_chunk_separator_char)
    lateinit var emojiIdChunkSeparator: String
    @BindString(R.string.create_wallet_your_emoji_id_text_label)
    lateinit var thisIsYourEmojiIdTitle: String
    @BindString(R.string.create_wallet_your_emoji_id_text_label_bold_part)
    lateinit var thisIsYourEmojiIdTitleBoldPart: String

    @Inject
    lateinit var wallet: FFITestWallet
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    @Named(WalletModule.FieldName.walletFilesDirPath)
    lateinit var walletFilesDirPath: String
    @JvmField
    @field:[Inject Named(ConfigModule.FieldName.receiveFromAnonymous)]
    var createNewWalletReceiveFromAnonymous: Boolean = false
    @JvmField
    @field:[Inject Named(ConfigModule.FieldName.generateTestData)]
    var createNewWalletGenerateTestData: Boolean = false
    @Inject
    lateinit var tracker: Tracker

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private val uiHandler = Handler(Looper.getMainLooper())
    private val halfSecondMs = 500L
    private var listener: Listener? = null

    override val contentViewId = R.layout.fragment_create_wallet

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUi()

        TrackHelper.track()
            .screen("/onboarding/create_wallet")
            .title("Onboarding - Create Wallet")
            .with(tracker)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacksAndMessages(null)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            listener = activity as Listener
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun setupUi() {
        val mActivity = activity ?: return
        yourEmojiIdTitleTextView.text = thisIsYourEmojiIdTitle.applyFontStyle(
            mActivity,
            CustomFont.AVENIR_LT_STD_LIGHT,
            thisIsYourEmojiIdTitleBoldPart,
            CustomFont.AVENIR_LT_STD_BLACK
        )

        bottomSpinnerLottieAnim.alpha = 0f
        val emojiId = sharedPrefsWrapper.emojiId!!
        emojiIdTextView.text = EmojiUtil.getChunkedEmojiId(
            emojiId,
            emojiIdChunkSeparator
        )
        OverScrollDecoratorHelper.setUpOverScroll(emojiIdScrollView)
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiIdSummaryView)
        emojiIdSummaryController.display(emojiId)

        seeFullEmojiIdButtonContainerView.visibility = View.INVISIBLE
        emojiIdSummaryContainerView.visibility = View.INVISIBLE
        emojiIdContainerView.visibility = View.INVISIBLE
        tapToSeeFullEmojiIdButton.isEnabled = false

        continueButton.alpha = 0f
        createEmojiIdButton.alpha = 0f
        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    whiteBgView.translationY = -whiteBgView.height.toFloat()
                    playStartupWhiteBgAnimation()
                    UiUtil.setBottomMargin(
                        createEmojiIdButton,
                        createEmojiIdButton.height * -2
                    )
                    UiUtil.setBottomMargin(
                        continueButton,
                        continueButton.height * -2
                    )
                }
            })

        checkMarkAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                startCreateEmojiAnimation()
            }
        })
    }

    private fun playStartupWhiteBgAnimation() {
        val whiteBgViewAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                whiteBgView,
                View.TRANSLATION_Y,
                -whiteBgView.height.toFloat(),
                0f
            )
        whiteBgViewAnim.duration = CreateEmojiId.whiteBgAnimDurationMs
        whiteBgViewAnim.interpolator = EasingInterpolator(Ease.SINE_OUT)
        whiteBgViewAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                showBottomSpinner()
                startInitHelloTextAnimation()
            }

            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                smallGemImageView.visibility = View.VISIBLE
                whiteBgView.visibility = View.VISIBLE
            }
        })
        whiteBgViewAnim.start()
    }

    private fun startInitHelloTextAnimation() {
        val offset = -helloText.height.toFloat()
        val helloTextAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(helloText, View.TRANSLATION_Y, 0f, offset)

        helloTextAnim.duration = CreateEmojiId.helloTextAnimDurationMs
        helloTextAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)

        helloTextAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                helloTextBackView.visibility = View.VISIBLE
                helloText.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                uiHandler.postDelayed({
                    helloTextBackView.visibility = View.GONE
                    justSecDescBackView.visibility = View.VISIBLE
                    justSecTitleBackView.visibility = View.VISIBLE
                    startSecondViewTextAnimation()
                }, CreateEmojiId.viewChangeAnimDelayMs)
            }
        })
        helloTextAnim.start()
    }

    private fun showBottomSpinner() {
        ObjectAnimator.ofFloat(bottomSpinnerLottieAnim, "alpha", 0f, 1f).run {
            duration = Constants.UI.longDurationMs
            start()
        }
    }

    private fun hideLoaderVideo() {
        ObjectAnimator.ofFloat(bottomSpinnerLottieAnim, "alpha", 1f, 0f).run {
            duration = CreateEmojiId.shortAlphaAnimDuration
            start()
        }
    }

    @OnClick(R.id.create_wallet_btn_continue)
    fun onContinueButtonClick() {
        UiUtil.temporarilyDisableClick(continueButton)
        sharedPrefsWrapper.onboardingCompleted = true
        val animatorSet = UiUtil.animateButtonClick(continueButton)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                sharedPrefsWrapper.onboardingAuthSetupStarted = true
                listener?.continueToEnableAuth()
            }
        })
    }

    @OnClick(R.id.create_wallet_btn_create_emoji_id)
    fun onCreateEmojiIdButtonClick() {
        UiUtil.temporarilyDisableClick(createEmojiIdButton)
        val animatorSet = UiUtil.animateButtonClick(createEmojiIdButton)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                showEmojiWheelAnimation()
            }
        })
    }

    private fun showEmojiWheelAnimation() {
        emojiWheelAnimView.playAnimation()

        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            nerdFaceEmojiLottieAnim.alpha = alpha
            createYourEmojiIdLine1TextView.alpha = alpha
            createYourEmojiIdLine2TextView.alpha = alpha
            walletAddressDescText.alpha = alpha
            createEmojiIdButton.alpha = alpha
        }
        fadeOutAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                createEmojiIdButton.visibility = View.GONE
                awesomeTextBackView.visibility = View.GONE
                justSecTitleBackView.visibility = View.GONE
                justSecDescBackView.visibility = View.GONE
                createEmojiIdTextBackView.visibility = View.GONE
            }
        })

        fadeOutAnim.startDelay = CreateEmojiId.walletCreationFadeOutAnimDelayMs
        fadeOutAnim.duration = CreateEmojiId.walletCreationFadeOutAnimDurationMs
        fadeOutAnim.start()

        uiHandler.postDelayed(
            { startYourEmojiIdViewAnimation() },
            emojiWheelAnimView.duration - CreateEmojiId.awesomeTextAnimDurationMs
        )
    }

    private fun startYourEmojiIdViewAnimation() {
        val buttonInitialBottomMargin = UiUtil.getBottomMargin(continueButton)
        val buttonBottomMarginDelta = createEmojiButtonBottomMargin - buttonInitialBottomMargin
        val buttonTranslationAnim = ValueAnimator.ofFloat(0f, 1f)
        buttonTranslationAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setBottomMargin(
                continueButton,
                (buttonInitialBottomMargin + buttonBottomMarginDelta * value).toInt()
            )
        }

        val buttonFadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        buttonFadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            continueButton.alpha = alpha
        }

        val buttonAnimSet = AnimatorSet()
        buttonAnimSet.playTogether(buttonTranslationAnim, buttonFadeInAnim)
        buttonAnimSet.duration = CreateEmojiId.continueButtonAnimDurationMs

        val emojiContainerImageScaleAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiContainerImageScaleAnim.addUpdateListener { animation ->
            val value = animation.animatedValue.toString().toFloat()
            val scale = 1.0f + (1f - value) * 0.5f
            emojiIdSummaryContainerView.scaleX = scale
            emojiIdSummaryContainerView.scaleY = scale
        }
        emojiContainerImageScaleAnim.startDelay = CreateEmojiId.emojiIdImageViewAnimDelayMs

        val titleOffset = -(yourEmojiIdTitleContainerView.height).toFloat()
        val yourEmojiTitleAnim =
            ObjectAnimator.ofFloat(yourEmojiIdTitleContainerView, View.TRANSLATION_Y, 0f, titleOffset)
        yourEmojiTitleAnim.startDelay = CreateEmojiId.yourEmojiIdTextAnimDelayMs
        yourEmojiTitleAnim.duration = CreateEmojiId.yourEmojiIdTextAnimDurationMs
        yourEmojiTitleAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                yourEmojiTitleBackView.visibility = View.VISIBLE
                yourEmojiIdTitleContainerView.visibility = View.VISIBLE
            }
        })

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            emojiIdSummaryContainerView.alpha = alpha
            emojiIdDescriptionTextView.alpha = alpha
        }
        fadeInAnim.startDelay = CreateEmojiId.emojiIdImageViewAnimDelayMs
        fadeInAnim.duration = CreateEmojiId.continueButtonAnimDurationMs

        val animSet = AnimatorSet()
        animSet.playTogether(
            buttonAnimSet,
            emojiContainerImageScaleAnim,
            fadeInAnim,
            yourEmojiTitleAnim
        )
        animSet.duration = CreateEmojiId.emojiIdCreationViewAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUINT_IN)

        seeFullEmojiIdButtonContainerView.alpha = 0f
        seeFullEmojiIdButtonContainerView.visibility = View.VISIBLE
        emojiIdSummaryContainerView.visibility = View.VISIBLE
        animSet.start()
        animSet.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                elevateEmojiIdSummaryAndShowSeeFulEmojiIdButton()
            }
        })
    }

    private fun elevateEmojiIdSummaryAndShowSeeFulEmojiIdButton() {
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            emojiIdSummaryContainerView.elevation = value * emojiIdContainerElevation
            UiUtil.setTopMargin(
                seeFullEmojiIdButtonContainerView,
                (seeFullEmojiIdButtonVisibleTopMargin * value).toInt()
            )
            seeFullEmojiIdButtonContainerView.alpha = value
        }
        anim.duration = Constants.UI.mediumDurationMs
        anim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        anim.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                tapToSeeFullEmojiIdButton.isEnabled = true
            }
        })
        anim.start()
    }

    @OnClick(R.id.create_wallet_btn_see_full_emoji_id)
    fun onSeeFullEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        // prepare views
        emojiIdSummaryContainerView.visibility = View.INVISIBLE
        val fullEmojiIdInitialWidth = emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = (rootView.width - horizontalMargin * 2) - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            emojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        emojiIdContainerView.alpha = 0f
        emojiIdContainerView.visibility = View.VISIBLE
        // scroll to end
        emojiIdScrollView.post {
            emojiIdScrollView.scrollTo(
                emojiIdTextView.width - emojiIdScrollView.width,
                0
            )
        }
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // container alpha & scale
            emojiIdContainerView.alpha = value
            UiUtil.setWidth(
                emojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
            UiUtil.setTopMargin(
                seeFullEmojiIdButtonContainerView,
                (seeFullEmojiIdButtonVisibleTopMargin * (1f - value)).toInt()
            )
            seeFullEmojiIdButtonContainerView.alpha = 1f - value
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        emojiIdAnim.start()
        emojiIdAnim.addListener(object: AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                seeFullEmojiIdButtonContainerView.visibility = View.INVISIBLE
                emojiIdScrollView.postDelayed({
                    emojiIdScrollView.smoothScrollTo(0, 0)
                }, Constants.UI.shortDurationMs + 20)
            }
        })
    }

    private fun startCreateEmojiAnimation() {
        // animation is looping, so we have to skip the fade-in and scale-up anims
        // that happen at the beginning of the animation
        nerdFaceEmojiLottieAnim.setMinFrame(50)
        nerdFaceEmojiLottieAnim.translationY = -(nerdFaceEmojiLottieAnim.height).toFloat()
        nerdFaceEmojiLottieAnim.playAnimation()

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            walletAddressDescText.alpha = alpha
            createEmojiIdButton.alpha = alpha
            nerdFaceEmojiLottieAnim.alpha = alpha
            nerdFaceEmojiLottieAnim.alpha = alpha
        }

        val createNowAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                createYourEmojiIdLine2TextView,
                View.TRANSLATION_Y,
                0f,
                -createYourEmojiIdLine2TextView.height.toFloat()
            )
        createNowAnim.duration = CreateEmojiId.awesomeTextAnimDurationMs

        val awesomeAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                createYourEmojiIdLine1TextView,
                View.TRANSLATION_Y,
                0f,
                -createYourEmojiIdLine1TextView.height.toFloat()
            )
        awesomeAnim.duration = CreateEmojiId.awesomeTextAnimDurationMs

        val buttonInitialBottomMargin = UiUtil.getBottomMargin(createEmojiIdButton)
        val buttonBottomMarginDelta = createEmojiButtonBottomMargin - buttonInitialBottomMargin
        val buttonTranslationAnim = ValueAnimator.ofFloat(0f, 1f)
        buttonTranslationAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            UiUtil.setBottomMargin(
                createEmojiIdButton,
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
                createEmojiIdTextBackView.visibility = View.VISIBLE
                awesomeTextBackView.visibility = View.VISIBLE
                createYourEmojiIdLine1TextView.visibility = View.VISIBLE
                createYourEmojiIdLine2TextView.visibility = View.VISIBLE
                helloTextBackView.visibility = View.GONE
                justSecDescBackView.visibility = View.GONE
                justSecTitleBackView.visibility = View.GONE
            }
        })
        animSet.start()
    }

    private fun startSecondViewTextAnimation() {
        val helloTextFadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        helloTextFadeOutAnim.duration = CreateEmojiId.shortAlphaAnimDuration



        helloTextFadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            helloText.alpha = alpha
        }

        helloTextFadeOutAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                helloText.visibility = View.GONE
            }
        })
        helloTextFadeOutAnim.start()
        uiHandler.postDelayed(
            { showSecondViewByAnim() },
            CreateEmojiId.viewOverlapDelayMs
        )
    }

    private fun showSecondViewByAnim() {
        val offset = -justSecTitle.height.toFloat()
        val titleAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(justSecTitle, View.TRANSLATION_Y, 0f, offset)
        titleAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)
        titleAnim.startDelay = CreateEmojiId.titleShortAnimDelayMs
        val descAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(justSecDescText, View.TRANSLATION_Y, 0f, offset)
        descAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)

        val animSet = AnimatorSet()
        animSet.playTogether(titleAnim, descAnim)
        animSet.duration = CreateEmojiId.helloTextAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                justSecDescText.visibility = View.VISIBLE
                justSecTitle.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                AsyncTask.execute {
                    generateTestData()
                }
            }
        })
        animSet.start()
    }

    private fun generateTestData() {
        var elapsedTime = 0L
        try {
            val startTime = System.currentTimeMillis()
            if (createNewWalletReceiveFromAnonymous) {
                for (i in 0 until 3) {
                    if (!wallet.testReceiveTx()) {
                        throw RuntimeException()
                    }
                    Thread.sleep(halfSecondMs)
                }
            }
            if (createNewWalletGenerateTestData) {
                if (!wallet.generateTestData(walletFilesDirPath)) {
                    throw RuntimeException()
                }
            }
            elapsedTime = System.currentTimeMillis() - startTime
        } catch (throwable: Throwable) {
            // silent fail
        }
        uiHandler.postDelayed(
            {
                startCheckMarkAnimation()
                hideLoaderVideo()
            },
            max(0L, (CreateEmojiId.viewChangeAnimDelayMs - elapsedTime))

        )
    }

    private fun startCheckMarkAnimation() {
        val fadeOut = ValueAnimator.ofFloat(1f, 0f)
        fadeOut.duration = CreateEmojiId.shortAlphaAnimDuration
        fadeOut.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            justSecDescText.alpha = alpha
            justSecTitle.alpha = alpha
        }

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                checkMarkAnim.visibility = View.VISIBLE

            }
        })

        fadeOut.start()
        checkMarkAnim.playAnimation()
    }

    fun fadeOutAllViewAnimation() {
        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        val emojiIdViewToFadeOut = when(emojiIdContainerView.visibility) {
            View.VISIBLE -> emojiIdContainerView
            else -> emojiIdSummaryContainerView
        }
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            continueButton.alpha = alpha
            emojiIdDescriptionTextView.alpha = alpha
            seeFullEmojiIdButtonContainerView.alpha = alpha
            emojiIdViewToFadeOut.alpha = alpha

            yourEmojiIdTitleContainerView.alpha = alpha
        }
        fadeOutAnim.duration = CreateEmojiId.fadeOutAnimDurationMs
        fadeOutAnim.start()
    }

    interface Listener {
        fun continueToEnableAuth()
    }
}