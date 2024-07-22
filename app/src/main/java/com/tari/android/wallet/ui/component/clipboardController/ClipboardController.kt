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
package com.tari.android.wallet.ui.component.clipboardController

import android.animation.AnimatorSet
import android.animation.LayoutTransition
import android.animation.ValueAnimator
import android.app.Activity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.animation.addListener
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ViewClipboardWalletBinding
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.hideKeyboard
import com.tari.android.wallet.ui.extension.setBottomMargin
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil

/**
 * Used to display the emoji id "copied" animation in emoji id views.
 *
 * @author The Tari Development Team
 */
class ClipboardController(
    private val localDimmers: List<View>,
    private val ui: ViewClipboardWalletBinding,
    private val viewModel: WalletAddressViewModel
) {

    private val context = ui.root.context
    private val activity = ui.root.context as Activity

    var hidePasteEmojiIdViewsOnTextChanged = false

    var listener: ClipboardControllerListener? = null


    private val dimmerViews
        get() = arrayOf(ui.bottomDimmerView) + localDimmers

    init {
        hidePasteEmojiIdViews(animate = false)
        dimmerViews.forEach { it.setOnClickListener { onEmojiIdDimmerClicked() } }
        ui.pasteEmojiIdButton.setOnClickListener { onPasteEmojiIdButtonClicked() }
        ui.emojiIdTextView.setOnClickListener { onPasteEmojiIdButtonClicked() }

        dimmerViews.forEach {
            (it.parent as? ConstraintLayout)?.layoutTransition?.disableTransitionType(LayoutTransition.DISAPPEARING)
            (it.parent as? ConstraintLayout)?.layoutTransition?.disableTransitionType(LayoutTransition.APPEARING)
        }
    }


    fun showClipboardData(data: TariWalletAddress) {
        ui.root.postDelayed({
            hidePasteEmojiIdViewsOnTextChanged = true
            showPasteEmojiIdViews(data.fullEmojiId)
            listener?.focusOnEditText(true)
        }, 100)
    }

    /**
     * Displays paste-emoji-id-related views.
     */
    private fun showPasteEmojiIdViews(emojiId: String) {
        ui.emojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            emojiId = emojiId,
            separator = context.string(R.string.emoji_id_chunk_separator),
            darkColor = PaletteManager.getBlack(context),
            lightColor = PaletteManager.getLightGray(context)
        )
        ui.emojiIdContainerView.setBottomMargin(-context.dimenPx(R.dimen.add_recipient_clipboard_emoji_id_container_height))
        ui.emojiIdContainerView.visible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }

        // animate
        val emojiIdAppearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val animValue = valueAnimator.animatedValue as Float
                dimmerViews.forEach { dimmerView -> dimmerView.alpha = animValue * 0.6f }
                ui.emojiIdContainerView.setBottomMargin((-context.dimenPx(R.dimen.add_recipient_clipboard_emoji_id_container_height) * (1f - animValue)).toInt())
            }
            interpolator = EasingInterpolator(Ease.EASE_IN_OUT_EXPO)
            duration = Constants.UI.mediumDurationMs
        }


        // animate and show paste emoji id button
        ui.pasteEmojiIdContainerView.setTopMargin(0)
        val pasteButtonAppearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.pasteEmojiIdContainerView.setTopMargin(
                    (context.dimenPx(R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin) * value).toInt()
                )
                ui.pasteEmojiIdContainerView.alpha = value
            }
            addListener(onStart = { ui.pasteEmojiIdContainerView.visible() })
            interpolator = EasingInterpolator(Ease.BACK_OUT)
            duration = Constants.UI.shortDurationMs
        }

        AnimatorSet().apply {
            playSequentially(emojiIdAppearAnim, pasteButtonAppearAnim)
            startDelay = Constants.UI.xShortDurationMs
            start()
        }
    }

    /**
     * Paste banner clicked.
     */
    private fun onPasteEmojiIdButtonClicked() {
        hidePasteEmojiIdViewsOnTextChanged = false
        hidePasteEmojiIdViews(animate = true) {
            listener?.onPaste(viewModel.discoveredWalletAddressFromClipboard.value!!)
        }
    }

    private fun onEmojiIdDimmerClicked() {
        hidePasteEmojiIdViews(animate = true) {
            activity.hideKeyboard()
            listener?.focusOnEditText(false)
        }
    }

    fun hidePasteEmojiIdViews(animate: Boolean, onEnd: (() -> (Unit))? = null) {
        if (!animate) {
            ui.pasteEmojiIdContainerView.gone()
            ui.emojiIdContainerView.gone()
            dimmerViews.forEach(View::gone)
            onEnd?.let { it() }
            return
        }
        // animate and hide paste emoji id button
        val pasteButtonDisappearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                ui.pasteEmojiIdContainerView.setTopMargin(
                    (context.dimenPx(R.dimen.add_recipient_paste_emoji_id_button_visible_top_margin) * (1 - value)).toInt()
                )
                ui.pasteEmojiIdContainerView.alpha = (1 - value)
            }
            addListener(onEnd = { ui.pasteEmojiIdContainerView.gone() })
            duration = Constants.UI.shortDurationMs
        }
        // animate and hide emoji id & dimmers
        val emojiIdDisappearAnim = ValueAnimator.ofFloat(0f, 1f).apply {
            addUpdateListener { valueAnimator: ValueAnimator ->
                val value = valueAnimator.animatedValue as Float
                dimmerViews.forEach { dimmerView -> dimmerView.alpha = 0.6f * (1 - value) }
                ui.emojiIdContainerView.setBottomMargin((-context.dimenPx(R.dimen.add_recipient_clipboard_emoji_id_container_height) * value).toInt())
            }
            addListener(onEnd = {
                ui.emojiIdContainerView.gone()
                dimmerViews.forEach { it.gone() }
            })
            duration = Constants.UI.shortDurationMs
        }

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(pasteButtonDisappearAnim, emojiIdDisappearAnim)
        if (onEnd != null) {
            animSet.addListener(onEnd = { onEnd() })
        }
        animSet.start()
    }

    interface ClipboardControllerListener {
        fun onPaste(walletAddress: TariWalletAddress)

        fun focusOnEditText(isFocused: Boolean)
    }
}
