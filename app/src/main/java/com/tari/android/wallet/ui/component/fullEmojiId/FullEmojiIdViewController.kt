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
package com.tari.android.wallet.ui.component.fullEmojiId

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.View
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewEmojiIdSummaryBinding
import com.tari.android.wallet.databinding.ViewFullEmojiIdBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.doOnGlobalLayout
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.setBottomMargin
import com.tari.android.wallet.ui.extension.setLayoutHeight
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.EmojiUtil.Companion.getGraphemeLength
import com.tari.android.wallet.util.EmojiUtil.Companion.SMALL_EMOJI_ID_SIZE
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper

/**
 * Used to display the full emoji id
 *
 * @author The Tari Development Team
 */
class FullEmojiIdViewController(
    private val ui: ViewFullEmojiIdBinding,
    summary: ViewEmojiIdSummaryBinding,
    private val context: Context,
    private val listener: Listener? = null
) {
    private val emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
    private val paletteManager = PaletteManager()
    private val summaryParent = summary.root.parent as View
    private var _fullEmojiId = ""

    var fullEmojiId: String
        set(value) {
            _fullEmojiId = value
            displayFullEmojiId()
        }
        get() = _fullEmojiId
    var emojiIdHex: String = ""

    init {
        ui.dimmerView.setOnClickListener { hideFullEmojiIdAnimated() }
        ui.copyEmojiIdButton.setOnClickListener { onCopyEmojiIdButtonClicked() }
        ui.copyEmojiIdButton.setOnLongClickListener { onCopyEmojiIdButtonLongClicked() }

        OverScrollDecoratorHelper.setUpOverScroll(ui.fullEmojiIdScrollView)

        hideFullEmojiIdWithoutAnimation()
    }

    // region Show animations

    fun showFullEmojiId() = with(ui) {
        listener?.onShowAnimated()

        prepareViewsToShow()

        val emojiIdAnim = getFullEmojiIdAnimation(true)

        val copyEmojiIdButtonAnim = getCopyButtonAnimation(true)

        val longPressHintAnimation = getLongPressHintAnimation(true)

        val showAnimation = AnimatorSet().apply {
            playTogether(copyEmojiIdButtonAnim, longPressHintAnimation)
        }

        AnimatorSet().apply {
            playSequentially(emojiIdAnim, showAnimation)
            addListener(onEnd = { dimmerView.isClickable = true })
            start()
        }

        smoothScrollToStart()
    }

    private fun prepareViewsToShow() = with(ui) {
        // make dimmers non-clickable until the anim is over
        dimmerView.visibleAndZeroAlpha()
        dimmerView.isClickable = false

        copyEmojiIdButton.isEnabled = true

        summaryParent.invisible()
        emojiIdOuterContainer.visible()

        ui.fullEmojiIdContainerView.doOnGlobalLayout {
            // get right position on the screen with considering scrolls, offsets and e.t.c
            val locationSummary = IntArray(2)
            summaryParent.getLocationOnScreen(locationSummary)

            val parentLocation = IntArray(2)
            ui.emojiIdOuterContainer.getLocationOnScreen(parentLocation)
            val topOffset = locationSummary[1] - parentLocation[1]

            ui.fullEmojiIdContainerView.apply {
                setTopMargin(topOffset)
                setLayoutHeight(summaryParent.height)
                setLayoutWidth(summaryParent.width)
            }
        }


        fullEmojiIdContainerView.visibleAndZeroAlpha()
        fullEmojiIdContainerView.setLayoutWidth(summaryParent.width)

        copyEmojiIdButtonContainerView.visibleAndZeroAlpha()
        copyEmojiIdButtonContainerView.setBottomMargin(0)

        longPressHintContainer.visible()
        longPressHintContainer.translationY = 0f

        smoothScrollToEnd()
    }

    // endregion Show animations


    // region Hide animations
    fun hideFullEmojiIdAnimated(animateCopyEmojiIdButton: Boolean = true) = with(ui) {
        listener?.onHide(true)

        prepareViewsToHide()

        val emojiIdAnim = getFullEmojiIdAnimation(false)

        val copyEmojiIdButtonAnim = getCopyButtonAnimation(false)

        val longPressHintAnimation = getLongPressHintAnimation(false)

        val hideAnimations = AnimatorSet().apply {
            playTogether(emojiIdAnim, longPressHintAnimation)
        }

        AnimatorSet().apply {
            if (animateCopyEmojiIdButton) {
                playSequentially(copyEmojiIdButtonAnim, hideAnimations)
            } else {
                play(hideAnimations)
            }
            addListener(onEnd = { hideFullEmojiIdWithoutAnimation() })
            start()
        }
    }

    fun hideFullEmojiIdWithoutAnimation() = with(ui) {
        listener?.onHide(false)

        dimmerView.gone()
        fullEmojiIdContainerView
        copyEmojiIdButtonContainerView.gone()
        emojiIdOuterContainer.invisible()
        longPressHintContainer.invisible()
    }

    private fun prepareViewsToHide() = with(ui) {
        fullEmojiIdScrollView.smoothScrollTo(0, 0)
        summaryParent.visible()
        summaryParent.alpha = 0f
        dimmerView.isClickable = false
        copyEmojiIdButton.isClickable = false
    }

    // endregion Hide animations


    // region Animations

    private fun getFullEmojiIdAnimation(isShow: Boolean) = with(ui) {
        val fullEmojiIdDeltaWidth = (emojiIdOuterContainer.width - context.dimenPx(R.dimen.common_horizontal_margin) * 2) - summaryParent.width

        val start = if (isShow) 0f else 1f
        val end = if (isShow) 1f else 0f

        return@with ValueAnimator.ofFloat(start, end).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                if (isShow) {
                    val scaleValue = 1f + 0.2f * (1f - value)
                    fullEmojiIdContainerView.scaleX = scaleValue
                    fullEmojiIdContainerView.scaleY = scaleValue
                }
                dimmerView.alpha = value * 0.6f
                summaryParent.alpha = (1 - value)
                fullEmojiIdContainerView.alpha = value

                val width = (summaryParent.width + fullEmojiIdDeltaWidth * value).toInt()
                fullEmojiIdContainerView.setLayoutWidth(width)

                if (isShow) {
                    listener?.animationShow(value)
                } else {
                    listener?.animationHide(value)
                }
            }
            duration = Constants.UI.shortDurationMs
        }
    }

    private fun getCopyButtonAnimation(isShow: Boolean) = with(ui) {
        val start = if (isShow) 0f else 1f
        val end = if (isShow) 1f else 0f
        val easy = if (isShow) Ease.BACK_OUT else Ease.LINEAR
        return@with ValueAnimator.ofFloat(start, end).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                copyEmojiIdButtonContainerView.alpha = value
                copyEmojiIdButtonContainerView.setBottomMargin(
                    (context.dimenPx(R.dimen.common_copy_emoji_id_button_visible_bottom_margin) * value).toInt()
                )
            }
            duration = Constants.UI.shortDurationMs
            interpolator = EasingInterpolator(easy)
        }
    }

    private fun getLongPressHintAnimation(isShow: Boolean) = with(ui) {
        val start = if (isShow) 1f else 0f
        val end = if (isShow) 0f else 1f
        val parentHeight = emojiIdOuterContainer.height
        val offsetValue = parentHeight - longPressHintContainer.y + longPressHintContainer.height
        longPressHintContainer.translationY = if (isShow) offsetValue else 0f

        return@with ValueAnimator.ofFloat(start, end).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                longPressHintContainer.translationY = offsetValue * value
            }
            duration = Constants.UI.shortDurationMs
            interpolator = EasingInterpolator(Ease.LINEAR)
        }
    }

    private fun smoothScrollToStart() = with(ui.fullEmojiIdScrollView) {
        if (_fullEmojiId.getGraphemeLength() > SMALL_EMOJI_ID_SIZE) {
            postDelayed({ smoothScrollTo(0, 0) }, Constants.UI.shortDurationMs + 20)
        }
    }

    private fun smoothScrollToEnd() = with(ui.fullEmojiIdScrollView) {
        post { scrollTo(ui.fullEmojiIdTextView.width - width, 0) }
    }

    // endregion Animations


    private fun displayFullEmojiId() = with(context) {
        ui.fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            _fullEmojiId,
            string(R.string.emoji_id_chunk_separator),
            paletteManager.getBlack(context),
            paletteManager.getLightGray(context)
        )
    }


    // region Copy
    private fun onCopyEmojiIdButtonClicked() {
        completeCopyEmojiId(fullEmojiId)
    }

    private fun onCopyEmojiIdButtonLongClicked(): Boolean {
        completeCopyEmojiId(emojiIdHex)
        return true
    }

    private fun completeCopyEmojiId(clipboardString: String) = with(ui) {
        dimmerView.isClickable = false
        copyEmojiIdButton.isEnabled = false
        val clipBoard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText("Tari Wallet Identity", clipboardString)
        clipBoard?.setPrimaryClip(clipboardData)

        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiIdAnimated(false)
        }
        // hide copy emoji id button
        copyEmojiIdButtonContainerView.animate().alpha(0f).apply {
            Constants.UI.xShortDurationMs
            start()
        }
    }

    // endregion Copy

    private fun View.visibleAndZeroAlpha() {
        this.alpha = 0f
        this.visible()
    }

    interface Listener {
        fun onShowAnimated() = Unit

        fun onHide(animated: Boolean) = Unit

        fun animationHide(value: Float) = Unit

        fun animationShow(value: Float) = Unit
    }
}
