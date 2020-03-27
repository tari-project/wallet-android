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
package com.tari.android.wallet.ui.fragment.send

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.VideoView
import butterknife.BindString
import butterknife.BindView
import com.airbnb.lottie.LottieAnimationView
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.fragment.BaseFragment
import com.tari.android.wallet.ui.util.UiUtil.getResourceUri
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.WalletUtil
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Displays the successful outgoing transaction animation.
 *
 * @author The Tari Development Team
 */
class SendTxSuccessfulFragment : BaseFragment(), Animator.AnimatorListener {

    @BindView(R.id.send_tx_successful_vw_root)
    lateinit var rootView: View
    @BindView(R.id.send_tx_successful_video_bg)
    lateinit var videoView: VideoView
    @BindView(R.id.send_tx_successful_anim)
    lateinit var lottieAnimationView: LottieAnimationView
    @BindView(R.id.send_tx_successful_txt_info)
    lateinit var infoTextView: TextView
    @BindView(R.id.send_tx_successful_vw_info_container)
    lateinit var infoContainerView: View
    @BindString(R.string.send_tx_sucessful_info_format)
    lateinit var infoFormat: String
    @BindString(R.string.send_tx_sucessful_info_format_bold_part)
    lateinit var infoFormatBoldPart: String

    @Inject
    lateinit var tracker: Tracker

    /**
     * Tx properties.
     */
    private lateinit var recipientUser: User
    private lateinit var amount: MicroTari
    private lateinit var fee: MicroTari
    private lateinit var note: String

    private lateinit var listenerWR: WeakReference<Listener>
    private val wr = WeakReference(this)

    override val contentViewId: Int = R.layout.fragment_send_tx_successful

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // prepare fonts for partial bold text
        val mActivity = activity ?: return

        // get tx properties
        recipientUser = arguments!!.getParcelable("recipientUser")!!
        amount = arguments!!.getParcelable("amount")!!
        fee = arguments!!.getParcelable("fee")!!
        note = arguments!!.getString("note")!!

        // format spannable string
        val formattedAmount = if (amount.tariValue.toDouble() % 1 == 0.toDouble()) {
            amount.tariValue.toBigInteger().toString()
        } else {
            WalletUtil.amountFormatter.format(amount.tariValue)
        }
        val info = String.format(infoFormat, formattedAmount)
        val infoBoldPart = String.format(infoFormatBoldPart, formattedAmount)

        infoTextView.text = info.applyFontStyle(
            mActivity,
            CustomFont.AVENIR_LT_STD_LIGHT,
            infoBoldPart,
            CustomFont.AVENIR_LT_STD_BLACK,
            applyToOnlyFirstOccurence = true
        )
        infoTextView.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )
        infoTextView.visibility = View.INVISIBLE
        lottieAnimationView.addAnimatorListener(this)

        rootView.postDelayed(
            {
                wr.get()?.lottieAnimationView?.playAnimation()
                wr.get()?.playTextAppearAnimation()
            },
            Constants.UI.SendTxSuccessful.lottieAnimStartDelayMs
        )

        TrackHelper.track()
            .screen("/home/send_tari/successful")
            .title("Send Tari - Successful")
            .with(tracker)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listenerWR = WeakReference(context as Listener)
    }

    override fun onStart() {
        super.onStart()
        val mActivity = activity ?: return
        videoView.setVideoURI(mActivity.getResourceUri(R.raw.sending_background))
        videoView.setOnPreparedListener { mp -> mp.isLooping = true }
        videoView.start()
    }

    override fun onStop() {
        videoView.stopPlayback()
        super.onStop()
    }

    override fun onDestroy() {
        lottieAnimationView.removeAllAnimatorListeners()
        super.onDestroy()
    }

    private fun playTextAppearAnimation() {
        infoTextView.translationY = infoTextView.height.toFloat()
        infoTextView.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(
            infoTextView,
            "translationY",
            infoTextView.height.toFloat(),
            0f
        ).apply {
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.SendTxSuccessful.textAppearAnimStartDelayMs
            start()
        }

        ObjectAnimator.ofFloat(
            infoTextView,
            "alpha",
            1f,
            0f
        ).apply {
            duration = Constants.UI.longDurationMs
            interpolator = EasingInterpolator(Ease.QUART_IN_OUT)
            startDelay = Constants.UI.SendTxSuccessful.textFadeOutAnimStartDelayMs
            start()
        }
    }

    // region listener for the Lottie animation
    override fun onAnimationStart(animation: Animator?) {
        // no-op
    }

    override fun onAnimationRepeat(animation: Animator?) {
        // no-op
    }

    override fun onAnimationCancel(animation: Animator?) {
        // no-op
    }

    override fun onAnimationEnd(animation: Animator?) {
        lottieAnimationView.alpha = 0f
        listenerWR.get()?.sendTxCompleted(
            this,
            recipientUser,
            amount,
            fee,
            note
        )
    }
    //endregion Animator Listener

    /**
     * Listener interface - to be implemented by the host activity.
     */
    interface Listener {

        /**
         * Recipient is user.
         */
        fun sendTxCompleted(
            sourceFragment: SendTxSuccessfulFragment,
            recipientUser: User,
            amount: MicroTari,
            fee: MicroTari,
            note: String
        )

    }

}