/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.ui.util.UiUtil.getResourceUri
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.Constants.UI.CreateWallet

class CreateWalletActivity : BaseActivity() {

    @BindView(R.id.main_anim_tari)
    lateinit var tariAnimationView: LottieAnimationView
    @BindView(R.id.rootView)
    lateinit var rootView: FrameLayout
    @BindView(R.id.crypto_wallet_title_txt_line_1)
    lateinit var cryptoWalletTitleTextLine1: TextView
    @BindView(R.id.crypto_wallet_title_txt_line_2)
    lateinit var cryptoWalletTitleTextLine2: TextView
    @BindView(R.id.crypto_wallet_des_txt)
    lateinit var cryptoWalletDesText: TextView
    @BindView(R.id.create_wallet_btn)
    lateinit var createWalletButton: TextView
    @BindView(R.id.create_wallet_btn_layout)
    lateinit var walletBtnLayout: FrameLayout
    @BindView(R.id.viewContainer)
    lateinit var viewContainer: RelativeLayout
    @BindView(R.id.txt_testnet)
    lateinit var testnetTextView: TextView
    @BindView(R.id.main_img_small_gem)
    lateinit var smallGemImageView: ImageView
    @BindView(R.id.progress)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.videoView_loop)
    lateinit var videoView: VideoView
    @BindView(R.id.video_container)
    lateinit var videoContainer: FrameLayout

    @BindDimen(R.dimen.splash_screen_title_bottom_margin)
    @JvmField
    var cryptoTitleTextLine1BottomMargin = 0

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0

    override val contentViewId = R.layout.activity_create_wallet

    private val uiHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        init()
    }

    override fun onStart() {
        super.onStart()
        videoView.setVideoURI(getResourceUri(R.raw.splash_video_loop_1))
        videoView.start()
    }

    override fun onStop() {
        super.onStop()
        videoView.stopPlayback()
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun init() {
        rootView.post { startViewAnimation() }
        UiUtil.setProgressBarColor(progressBar, whiteColor)
        setVideoDimension()
    }

    private fun setVideoDimension() {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    val rootWidth = rootView.width
                    val viewHeight =
                        cryptoWalletTitleTextLine1.top - cryptoWalletTitleTextLine1.height - cryptoTitleTextLine1BottomMargin
                    val viewWidth = rootWidth.coerceAtMost(viewHeight) - tariAnimationView.top
                    val lp: FrameLayout.LayoutParams =
                        videoContainer.layoutParams as FrameLayout.LayoutParams

                    lp.width = viewWidth
                    lp.height = viewHeight - videoContainer.marginTop
                    videoContainer.layoutParams = lp
                }
            })
    }

    @OnClick(R.id.create_wallet_btn_layout)
    fun onCreateWalletClick() {
        createWalletButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        uiHandler.postDelayed({
            startTariViewAnimation()
        }, CreateWallet.tariTextAnimViewDurationMs)
    }

    private fun startTariViewAnimation() {
        val metrics = resources.displayMetrics
        val offset = (metrics.heightPixels / 2 - tariAnimationView.height).toFloat()
        val tariViewTranslateAnim =
            ObjectAnimator.ofFloat(tariAnimationView, View.TRANSLATION_Y, offset)
        tariViewTranslateAnim.interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)

        tariViewTranslateAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                tariAnimationView.playAnimation()
            }

            override fun onAnimationStart(animation: Animator?) {
                tariAnimationView.animate()
                    .scaleX(1f).scaleY(1f).setDuration(CreateWallet.tariTextAnimViewScaleDurationMs)
                    .start()
            }
        })

        val fadeOutViewAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutViewAnim.duration = Constants.UI.shortAnimDurationMs

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
        animSet.playTogether(tariViewTranslateAnim, fadeOutViewAnim)
        animSet.duration = CreateWallet.tariTextAnimViewDurationMs
        animSet.start()
    }

    private fun startViewAnimation() {
        val showTariTextFadeInAnim = ValueAnimator.ofFloat(0f, 1f)
        showTariTextFadeInAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            tariAnimationView.alpha = alpha
            testnetTextView.alpha = alpha
            smallGemImageView.alpha = alpha
            walletBtnLayout.alpha = alpha
            cryptoWalletDesText.alpha = alpha
            cryptoWalletTitleTextLine1.alpha = alpha
            cryptoWalletTitleTextLine2.alpha = alpha
        }

        val offset =
            -(cryptoWalletTitleTextLine1.height.toFloat() + cryptoTitleTextLine1BottomMargin)
        val mainTextAnim1: ObjectAnimator =
            ObjectAnimator.ofFloat(cryptoWalletTitleTextLine1, View.TRANSLATION_Y, 0f, offset)
        val mainTextAnim2: ObjectAnimator =
            ObjectAnimator.ofFloat(cryptoWalletTitleTextLine2, View.TRANSLATION_Y, 0f, offset)

        mainTextAnim2.startDelay = CreateWallet.cryptoTitleTextAnimDelayDurationMs

        val mainTextAnimSet = AnimatorSet()
        mainTextAnimSet.playTogether(mainTextAnim1, mainTextAnim2)
        mainTextAnimSet.interpolator = EasingInterpolator(Ease.SINE_OUT)

        val animSet = AnimatorSet()
        animSet.startDelay = Constants.UI.shortAnimDurationMs
        animSet.playTogether(showTariTextFadeInAnim, mainTextAnimSet)
        animSet.duration = CreateWallet.startUpAnimDuration
        animSet.start()
    }
}