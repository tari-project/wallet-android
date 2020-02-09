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
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import butterknife.BindDimen
import butterknife.BindView
import butterknife.OnClick
import com.airbnb.lottie.LottieAnimationView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.activity.AuthActivity
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.util.Constants

/**
 * onBoarding flow : wallet creation sequence
 *
 * @author The Tari Development Team
 */
class CreateEmojiIdFragment : BaseFragment() {

    @BindView(R.id.create_emoji_id_txt_hello)
    lateinit var helloText: TextView
    @BindView(R.id.create_emoji_id_txt_just_sec_desc)
    lateinit var justSecDescText: TextView
    @BindView(R.id.create_emoji_id_txt_just_sec_title)
    lateinit var justSecTitle: TextView
    @BindView(R.id.create_emoji_id_hello_text_back_view)
    lateinit var helloTextBackView: View
    @BindView(R.id.create_emoji_id_checkmark_anim)
    lateinit var checkMarkAnim: LottieAnimationView
    @BindView(R.id.create_emoji_id_txt_wallet_address_des)
    lateinit var walletAddressDescText: TextView
    @BindView(R.id.create_emoji_id_continue_btn)
    lateinit var createEmojiIdButton: Button
    @BindView(R.id.create_emoji_id_just_sec_back_view)
    lateinit var justSecBackView: View
    @BindView(R.id.create_emoji_id_nerd_face_emoji)
    lateinit var nerdFaceEmoji: LottieAnimationView
    @BindView(R.id.create_emoji_id_txt_create_your_emoji_id)
    lateinit var createYourEmojiIdText: TextView
    @BindView(R.id.create_emoji_id_text_back_view)
    lateinit var createEmojiIdTextBackView: View
    @BindView(R.id.create_emoji_id_txt_awesome)
    lateinit var awesomeText: TextView
    @BindView(R.id.create_emoji_id_white_bg_view)
    lateinit var whiteBgView: View
    @BindView(R.id.create_emoji_id_awesome_text_back_view)
    lateinit var awesomeTextBackView: View

    @BindDimen(R.dimen.create_emoji_id_button_bottom_margin)
    @JvmField
    var createEmojiButtonBottomMargin = 0

    private val uiHandler = Handler()

    override val contentViewId = R.layout.fragment_create_emoji_id

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun setupUi() {
        whiteBgView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    whiteBgView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    whiteBgView.translationY = -whiteBgView.height.toFloat()
                    playStartupWhiteBgAnimation()
                }
            })

        checkMarkAnim.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                startCreateEmojiAnimation()
            }
        })

        nerdFaceEmoji.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                nerdFaceEmoji.playAnimation()
                nerdFaceEmoji.progress = 0.10f
            }
        })
    }

    @OnClick(R.id.create_emoji_id_continue_btn)
    fun openAuthActivity() {
        val intent = Intent(activity, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        activity?.finish()
    }

    private fun startCreateEmojiAnimation() {
        nerdFaceEmoji.translationY = -(nerdFaceEmoji.height).toFloat()
        nerdFaceEmoji.alpha = 1f
        nerdFaceEmoji.playAnimation()

        val fadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        fadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            walletAddressDescText.alpha = alpha
            createEmojiIdButton.alpha = alpha
        }

        val createNowAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                createYourEmojiIdText,
                View.TRANSLATION_Y,
                0f,
                -createYourEmojiIdText.height.toFloat()
            )
        createNowAnim.duration = Constants.UI.CreateEmojiId.awesomeTextAnimDurationMs

        val awesomeAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                awesomeText,
                View.TRANSLATION_Y,
                0f,
                -awesomeText.height.toFloat()
            )
        awesomeAnim.duration = Constants.UI.CreateEmojiId.awesomeTextAnimDurationMs

        val offset = -(createEmojiIdButton.height + createEmojiButtonBottomMargin).toFloat()
        val buttonAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(createEmojiIdButton, View.TRANSLATION_Y, 0f, offset)
        buttonAnim.duration = Constants.UI.CreateEmojiId.awesomeTextAnimDurationMs
        buttonAnim.startDelay = Constants.UI.CreateEmojiId.createEmojiButtonAnimDelayMs

        val animSet = AnimatorSet()
        animSet.playTogether(fadeInAnim, createNowAnim, awesomeAnim, buttonAnim)

        animSet.startDelay = Constants.UI.CreateEmojiId.viewOverlapDelayMs
        animSet.duration = Constants.UI.CreateEmojiId.createEmojiViewAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUINT_OUT)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                createEmojiIdTextBackView.visibility = View.VISIBLE
                awesomeTextBackView.visibility = View.VISIBLE
                awesomeText.visibility = View.VISIBLE
                createYourEmojiIdText.visibility = View.VISIBLE
                helloTextBackView.visibility = View.GONE
                justSecBackView.visibility = View.GONE
            }
        })
        animSet.start()
    }

    private fun startSecondViewTextAnimation() {
        val helloTextFadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        helloTextFadeOutAnim.duration = Constants.UI.CreateEmojiId.shortAlphaAnimDuration

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
            Constants.UI.CreateEmojiId.viewOverlapDelayMs
        )
    }

    private fun showSecondViewByAnim() {
        val offset = -justSecTitle.height.toFloat()
        val titleAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(justSecTitle, View.TRANSLATION_Y, 0f, offset)
        titleAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)
        titleAnim.startDelay = Constants.UI.CreateEmojiId.titleShortAnimDelayMs
        val descAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(justSecDescText, View.TRANSLATION_Y, 0f, offset)
        descAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)

        val animSet = AnimatorSet()
        animSet.playTogether(titleAnim, descAnim)
        animSet.duration = Constants.UI.CreateEmojiId.helloTextAnimDurationMs
        animSet.interpolator = EasingInterpolator(Ease.QUART_OUT)
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                justSecDescText.visibility = View.VISIBLE
                justSecTitle.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                uiHandler.postDelayed(
                    { startCheckMarkAnimation() },
                    Constants.UI.CreateEmojiId.viewChangeAnimDelayMs
                )
            }
        })
        animSet.start()
    }

    private fun startCheckMarkAnimation() {
        val fadeOut = ValueAnimator.ofFloat(1f, 0f)
        fadeOut.duration = Constants.UI.CreateEmojiId.shortAlphaAnimDuration
        fadeOut.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            justSecDescText.alpha = alpha
            justSecTitle.alpha = alpha
        }

        fadeOut.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?, isReverse: Boolean) {
                super.onAnimationEnd(animation, isReverse)
                checkMarkAnim.visibility = View.VISIBLE

            }
        })
        fadeOut.start()
        checkMarkAnim.playAnimation()
    }

    private fun playStartupWhiteBgAnimation() {
        val whiteBgViewAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(
                whiteBgView,
                View.TRANSLATION_Y,
                -whiteBgView.height.toFloat(),
                0f
            )

        whiteBgViewAnim.startDelay = Constants.UI.CreateEmojiId.whiteBgAnimDelayMs
        whiteBgViewAnim.duration = Constants.UI.CreateEmojiId.whiteBgAnimDurationMs
        whiteBgViewAnim.interpolator = EasingInterpolator(Ease.SINE_OUT)
        whiteBgViewAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                startInitHelloTextAnimation()
            }

            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                whiteBgView.visibility = View.VISIBLE
            }
        })
        whiteBgViewAnim.start()
    }

    private fun startInitHelloTextAnimation() {
        val offset = -helloText.height.toFloat()
        val helloTextAnim: ObjectAnimator =
            ObjectAnimator.ofFloat(helloText, View.TRANSLATION_Y, 0f, offset)

        helloTextAnim.duration = Constants.UI.CreateEmojiId.helloTextAnimDurationMs
        helloTextAnim.interpolator = EasingInterpolator(Ease.QUINT_OUT)

        helloTextAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                helloTextBackView.visibility = View.VISIBLE
                helloText.visibility = View.VISIBLE
                justSecBackView.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                uiHandler.postDelayed({
                    startSecondViewTextAnimation()
                }, Constants.UI.CreateEmojiId.viewChangeAnimDelayMs)
            }
        })
        helloTextAnim.start()
    }
}