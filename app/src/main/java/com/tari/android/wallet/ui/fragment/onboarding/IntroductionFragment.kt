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
import android.os.Handler
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import butterknife.*
import com.airbnb.lottie.LottieAnimationView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.applyURLStyle
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.ui.util.UiUtil.getResourceUri
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.min

/**
 * onBoarding flow : wallet introduction screen.
 * Private key and wallet do not get created in this fragment.
 * They get created in the next fragment (CreateWalletFragment) after the user clicks the CTA.
 *
 * @author The Tari Development Team
 */
internal class IntroductionFragment : BaseFragment() {

    @BindView(R.id.introduction_anim_tari)
    lateinit var tariWalletLottieAnimationView: LottieAnimationView
    @BindView(R.id.create_wallet_vw_root)
    lateinit var rootView: View
    @BindView(R.id.introduction_txt_header)
    lateinit var headerTextView: TextView
    @BindView(R.id.introduction_btn_create_wallet)
    lateinit var createWalletButton: TextView
    @BindView(R.id.introduction_btn_layout)
    lateinit var walletBtnLayout: FrameLayout
    @BindView(R.id.introduction_txt_testnet)
    lateinit var testnetTextView: TextView
    @BindView(R.id.introduction_img_small_gem)
    lateinit var smallGemImageView: ImageView
    @BindView(R.id.introduction_prog_create_wallet)
    lateinit var progressBar: ProgressBar
    @BindView(R.id.introduction_vw_video_outer_container)
    lateinit var videoOuterContainer: View
    @BindView(R.id.introduction_vw_video_inner_container)
    lateinit var videoInnerContainer: View
    @BindView(R.id.introduction_video_view)
    lateinit var videoView: VideoView
    @BindView(R.id.introduction_txt_user_agreement_and_privacy_policy)
    lateinit var userAgreementAndPrivacyPolicyTextView: TextView

    @BindDimen(R.dimen.splash_screen_title_bottom_margin)
    @JvmField
    var cryptoTitleTextLine1BottomMargin = 0

    @BindColor(R.color.white)
    @JvmField
    var whiteColor = 0

    @BindString(R.string.create_wallet_user_agreement_and_privacy_policy)
    lateinit var userAgreementAnPrivacyPolicyDesc: String
    @BindString(R.string.create_wallet_user_agreement)
    lateinit var userAgreement: String
    @BindString(R.string.create_wallet_user_agreement_url)
    lateinit var userAgreementURL: String
    @BindString(R.string.create_wallet_privacy_policy)
    lateinit var privacyPolicy: String
    @BindString(R.string.create_wallet_privacy_policy_url)
    lateinit var privacyPolicyURL: String

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var tracker: Tracker

    private var listener: Listener? = null

    private val handler = Handler()
    private val wr = WeakReference(this)

    private val createWalletArtificalDelay = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

    override val contentViewId = R.layout.fragment_introduction

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        TrackHelper.track()
            .screen("/onboarding/introduction")
            .title("Onboarding - Introduction")
            .with(tracker)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (activity is Listener) {
            listener = activity as Listener
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        listener = null
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacksAndMessages(null)
    }

    private var videoViewHasBeenSetup = false
    private var videoViewLastPosition = 0

    override fun onStart() {
        super.onStart()
        if (videoViewHasBeenSetup) {
            startVideo()
        }
    }

    override fun onPause() {
        videoViewLastPosition = videoView.currentPosition
        videoView.stopPlayback()
        super.onPause()
    }

    private fun setupUi() {
        UiUtil.setProgressBarColor(progressBar, whiteColor)

        tariWalletLottieAnimationView.alpha = 0f
        testnetTextView.alpha = 0f
        smallGemImageView.alpha = 0f
        walletBtnLayout.alpha = 0f
        headerTextView.alpha = 0f
        userAgreementAndPrivacyPolicyTextView.alpha = 0f

        // highlight links
        userAgreementAndPrivacyPolicyTextView.text =
            SpannableString(userAgreementAnPrivacyPolicyDesc).apply {
                applyURLStyle(
                    userAgreement,
                    userAgreementURL
                )
                applyURLStyle(
                    privacyPolicy,
                    privacyPolicyURL
                )
            }
        // make the links clickable
        userAgreementAndPrivacyPolicyTextView.movementMethod = LinkMovementMethod.getInstance()

        rootView.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    runStartupAnimation()
                    setupAndStartVideo()
                }
            })
    }

    private fun setupAndStartVideo() {
        val size = min(
            videoOuterContainer.width,
            videoOuterContainer.height
        )
        UiUtil.setWidthAndHeight(
            videoInnerContainer,
            size, size
        )
        videoView.setVideoURI(context!!.getResourceUri(R.raw.purple_orb))
        startVideo()
        videoViewHasBeenSetup = true
    }

    private fun startVideo() {
        videoView.setOnPreparedListener { mp ->
            mp.isLooping = true
            videoView.start()
            videoView.seekTo(videoViewLastPosition)
        }
    }

    private fun runStartupAnimation() {
        val headerHeight = headerTextView.measuredHeight
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            tariWalletLottieAnimationView.alpha = value
            testnetTextView.alpha = value
            smallGemImageView.alpha = value
            walletBtnLayout.alpha = value
            headerTextView.alpha = value
            userAgreementAndPrivacyPolicyTextView.alpha = value
            headerTextView.translationY = (1f - value) * headerHeight
        }

        anim.startDelay = Constants.UI.shortDurationMs
        anim.duration = Constants.UI.longDurationMs
        anim.interpolator = EasingInterpolator(Ease.SINE_IN_OUT)
        anim.start()
    }

    @OnClick(R.id.introduction_btn_create_wallet)
    fun onCreateWalletClick() {
        UiUtil.temporarilyDisableClick(createWalletButton)
        createWalletButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        rootView.postDelayed(
            { wr.get()?.startTariWalletViewAnimation() },
            createWalletArtificalDelay
        )
    }

    private fun startTariWalletViewAnimation() {
        val metrics = resources.displayMetrics
        val offset =
            (metrics.heightPixels / 2 - tariWalletLottieAnimationView.height / 2 - tariWalletLottieAnimationView.top).toFloat()
        val tariViewTranslateAnim = ObjectAnimator.ofFloat(
            tariWalletLottieAnimationView,
            View.TRANSLATION_Y,
            offset
        )
        tariViewTranslateAnim.interpolator = EasingInterpolator(Ease.CIRC_IN_OUT)
        tariViewTranslateAnim.duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs

        tariViewTranslateAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                tariWalletLottieAnimationView.playAnimation()
                handler.postDelayed(
                    { listener?.continueToCreateWallet() },
                    tariWalletLottieAnimationView.duration  // - Constants.UI.CreateWallet.showCreateEmojiIdWhiteBgDelayMs
                )
            }
        })

        val tariViewScaleAnim = ValueAnimator.ofFloat(
            tariWalletLottieAnimationView.scale,
            1f
        )
        tariViewScaleAnim.duration = Constants.UI.CreateWallet.tariTextAnimViewDurationMs
        tariViewScaleAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val scale = valueAnimator.animatedValue as Float
            tariWalletLottieAnimationView.scale = scale
        }

        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f)
        fadeOutAnim.duration = Constants.UI.CreateWallet.viewContainerFadeOutDurationMs
        fadeOutAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val alpha = valueAnimator.animatedValue as Float
            videoOuterContainer.alpha = alpha
            headerTextView.alpha = alpha
            walletBtnLayout.alpha = alpha
            userAgreementAndPrivacyPolicyTextView.alpha = alpha
        }
        fadeOutAnim.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                videoOuterContainer.visibility = View.INVISIBLE
                headerTextView.visibility = View.INVISIBLE
                walletBtnLayout.visibility = View.INVISIBLE
                userAgreementAndPrivacyPolicyTextView.visibility = View.INVISIBLE
            }
        })

        val animSet = AnimatorSet()
        animSet.playTogether(tariViewTranslateAnim, tariViewScaleAnim, fadeOutAnim)
        animSet.start()
    }

    interface Listener {

        fun continueToCreateWallet()

    }
}