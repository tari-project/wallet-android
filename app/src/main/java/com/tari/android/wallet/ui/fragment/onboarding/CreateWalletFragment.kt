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
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.core.view.marginTop
import butterknife.BindColor
import butterknife.BindDimen
import butterknife.BindView
import butterknife.OnClick
import com.airbnb.lottie.LottieAnimationView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.ui.util.UiUtil.getResourceUri
import com.tari.android.wallet.util.Constants

/**
 * onBoarding flow : wallet creation splash screen
 *
 * @author The Tari Development Team
 */
class CreateWalletFragment : BaseFragment() {

    @BindView(R.id.create_wallet_tari_wallet)
    lateinit var tariWalletView: LottieAnimationView
    @BindView(R.id.create_wallet_rootView)
    lateinit var rootView: FrameLayout
    @BindView(R.id.create_wallet_title_txt_line_1)
    lateinit var titleTextLine1: TextView
    @BindView(R.id.create_wallet_title_txt_line_2)
    lateinit var titleTextLine2: TextView
    @BindView(R.id.create_wallet_des_txt)
    lateinit var walletDesText: TextView
    @BindView(R.id.create_wallet_btn)
    lateinit var createWalletButton: TextView
    @BindView(R.id.create_wallet_btn_layout)
    lateinit var walletBtnLayout: FrameLayout
    @BindView(R.id.create_wallet_viewContainer)
    lateinit var viewContainer: RelativeLayout
    @BindView(R.id.create_wallet_txt_testnet)
    lateinit var testnetTextView: TextView
    @BindView(R.id.create_wallet_main_img_small_gem)
    lateinit var smallGemImageView: ImageView
    @BindView(R.id.create_wallet_progress)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.create_wallet_video_view)
    lateinit var videoView: VideoView
    @BindView(R.id.create_wallet_video_container)
    lateinit var videoContainer: FrameLayout

    @BindDimen(R.dimen.splash_screen_title_bottom_margin)
    @JvmField
    var cryptoTitleTextLine1BottomMargin = 0

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0

    private val uiHandler = Handler()

    override val contentViewId = R.layout.fragment_create_wallet

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        UiUtil.setProgressBarColor(progressBar, whiteColor)
    }

    private fun showCreateEmojiIdFragment() {
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.onboarding_create_emoji_id_container, CreateEmojiIdFragment())?.commit()

        removeCurrentFragment()
    }

    private fun removeCurrentFragment() {
        uiHandler.postDelayed({
            activity?.supportFragmentManager?.beginTransaction()
                ?.remove(this)?.commit()
        }, Constants.UI.CreateWallet.removeFragmentDelayDuration)
    }

    override fun onStart() {
        super.onStart()
        videoView.setVideoURI(context!!.getResourceUri(R.raw.splash_video_loop_1))
        videoView.start()
    }

    override fun onStop() {
        super.onStop()
        videoView.stopPlayback()
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun setupUi() {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    setVideoDimension()
                    runStartupAnimation()
                }
            })
    }

    private fun setVideoDimension() {
        val rootWidth = rootView.width
        val viewHeight =
            titleTextLine1.top - titleTextLine1.height - cryptoTitleTextLine1BottomMargin
        val viewWidth = rootWidth.coerceAtMost(viewHeight) - tariWalletView.top
        val lp: FrameLayout.LayoutParams =
            videoContainer.layoutParams as FrameLayout.LayoutParams

        lp.width = viewWidth
        lp.height = viewHeight - videoContainer.marginTop
        videoContainer.layoutParams = lp
    }

    @OnClick(R.id.create_wallet_btn)
    fun onCreateWalletClick() {
        UiUtil.temporarilyDisableClick(createWalletButton)
        createWalletButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        uiHandler.postDelayed({
            startTariWalletViewAnimation()
        }, Constants.UI.CreateWallet.tariTextAnimViewDurationMs)
    }

    private fun runStartupAnimation() {
        val showTariTextFadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        showTariTextFadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            tariWalletView.alpha = alpha
            testnetTextView.alpha = alpha
            smallGemImageView.alpha = alpha
            walletBtnLayout.alpha = alpha
            walletDesText.alpha = alpha
            titleTextLine1.alpha = alpha
            titleTextLine2.alpha = alpha
        }

        val offset =
            -(titleTextLine1.height.toFloat() + cryptoTitleTextLine1BottomMargin)
        val mainTextAnim1: ObjectAnimator =
            ObjectAnimator.ofFloat(titleTextLine1, View.TRANSLATION_Y, 0f, offset)
        val mainTextAnim2: ObjectAnimator =
            ObjectAnimator.ofFloat(titleTextLine2, View.TRANSLATION_Y, 0f, offset)

        mainTextAnim2.startDelay = Constants.UI.CreateWallet.titleTextAnimDelayDurationMs

        val mainTextAnimSet = AnimatorSet()
        mainTextAnimSet.playTogether(mainTextAnim1, mainTextAnim2)
        mainTextAnimSet.interpolator = EasingInterpolator(Ease.SINE_OUT)

        val animSet = AnimatorSet()
        animSet.startDelay = Constants.UI.shortAnimDurationMs
        animSet.playTogether(showTariTextFadeInAnim, mainTextAnimSet)
        animSet.duration = Constants.UI.CreateWallet.startUpAnimDuration
        animSet.start()
    }

    private fun startTariWalletViewAnimation() {
        val metrics = resources.displayMetrics
        val offset =
            (metrics.heightPixels / 2 - tariWalletView.top).toFloat()
        val tariViewTranslateAnim =
            ObjectAnimator.ofFloat(tariWalletView, View.TRANSLATION_Y, offset)
        tariViewTranslateAnim.interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
        tariViewTranslateAnim.duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

        tariViewTranslateAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                tariWalletView.playAnimation()
                uiHandler.postDelayed(
                    { showCreateEmojiIdFragment() },
                    tariWalletView.duration - Constants.UI.CreateWallet.showCreateEmojiIdWhiteBgDelayMs
                )
            }
        })

        val tariViewScaleAnim = ValueAnimator.ofFloat(0.8f, 1f)
        tariViewScaleAnim.duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

        tariViewScaleAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val scale = valueAnimator.animatedValue as Float
            tariWalletView.scaleX = scale
            tariWalletView.scaleY = scale
        }

        val fadeOutViewAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutViewAnim.duration = Constants.UI.CreateWallet.viewContainerFadeOutDurationMs

        fadeOutViewAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            viewContainer.alpha = alpha
            videoContainer.alpha = alpha
        }

        fadeOutViewAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                viewContainer.visibility = View.INVISIBLE
            }
        })

        val animSet = AnimatorSet()
        animSet.playTogether(tariViewTranslateAnim, tariViewScaleAnim, fadeOutViewAnim)
        animSet.start()
    }
}