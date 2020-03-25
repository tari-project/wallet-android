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
package com.tari.android.wallet.ui.component

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.util.Constants

/**
 * Used to display the emoji id "copied" animation in emoji id views.
 *
 * @author The Tari Development Team
 */
internal class EmojiIdCopiedViewController(view: View) {

    @BindView(R.id.emoji_id_copied_vw_white_bg)
    lateinit var whiteBgView: View
    @BindView(R.id.emoji_id_copied_vw_green_bg)
    lateinit var greenBgView: View
    @BindView(R.id.emoji_id_copied_txt_copied)
    lateinit var textView: TextView

    init {
        ButterKnife.bind(this, view)
        whiteBgView.post {
            whiteBgView.alpha = 0f
            greenBgView.alpha = 0f
            textView.alpha = 0f
        }
    }

    fun showEmojiIdCopiedAnim(fadeOutOnEnd: Boolean = false, then: (() -> Unit)? = null) {
        // fade in white bg
        val whiteBgFadeInAnim = ObjectAnimator.ofFloat(
            whiteBgView,
            "alpha",
            0f, 1f
        )
        whiteBgFadeInAnim.duration = Constants.UI.mediumDurationMs
        // fade in green bg
        val greenBgFadeInAnim = ObjectAnimator.ofFloat(
            greenBgView,
            "alpha",
            0f, 1f
        )
        greenBgFadeInAnim.duration = Constants.UI.mediumDurationMs
        // fade in text
        val textFadeInAnim = ObjectAnimator.ofFloat(
            textView,
            "alpha",
            0f, 1f
        )
        textFadeInAnim.startDelay = Constants.UI.shortDurationMs
        textFadeInAnim.duration = Constants.UI.mediumDurationMs
        // scale text
        val textScaleDownXAnim = ObjectAnimator.ofFloat(
            textView,
            "scaleX",
            3f, 1f
        )
        textScaleDownXAnim.startDelay = Constants.UI.shortDurationMs
        textScaleDownXAnim.duration = Constants.UI.mediumDurationMs
        textScaleDownXAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)
        val textScaleDownYAnim = ObjectAnimator.ofFloat(
            textView,
            "scaleY",
            3f, 1f
        )
        textScaleDownYAnim.startDelay = Constants.UI.shortDurationMs
        textScaleDownYAnim.duration = Constants.UI.mediumDurationMs
        textScaleDownYAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // fade out green bg
        val greenBgFadeOutAnim = ObjectAnimator.ofFloat(
            greenBgView,
            "alpha",
            1f, 0f
        )
        // it should be after everything shows
        greenBgFadeOutAnim.startDelay = Constants.UI.mediumDurationMs + Constants.UI.shortDurationMs
        greenBgFadeOutAnim.duration = Constants.UI.longDurationMs

        val animSet = AnimatorSet()
        animSet.playTogether(
            whiteBgFadeInAnim,
            greenBgFadeInAnim,
            textFadeInAnim,
            textScaleDownXAnim,
            textScaleDownYAnim,
            greenBgFadeOutAnim
        )
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                if (fadeOutOnEnd) {
                    fadeOut(then = then)
                    return
                }
                whiteBgView.alpha = 0f
                greenBgView.alpha = 0f
                textView.alpha = 0f
                then?.let { it() }
            }
        })
    }

    private fun fadeOut(then: (() -> Unit)? = null) {
        // fade out green bg
        val whiteBgFadeOutAnim = ObjectAnimator.ofFloat(
            whiteBgView,
            "alpha",
            1f, 0f
        )
        whiteBgFadeOutAnim.duration = Constants.UI.shortDurationMs
        val textViewFadeOutAnim = ObjectAnimator.ofFloat(
            textView,
            "alpha",
            1f, 0f
        )
        textViewFadeOutAnim.duration = Constants.UI.shortDurationMs

        val animSet = AnimatorSet()
        animSet.playTogether(whiteBgFadeOutAnim, textViewFadeOutAnim)
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                then?.let { it() }
            }
        })
    }

}