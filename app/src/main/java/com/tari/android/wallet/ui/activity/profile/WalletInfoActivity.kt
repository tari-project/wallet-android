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
package com.tari.android.wallet.ui.activity.profile

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import androidx.core.content.ContextCompat
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R.color.black
import com.tari.android.wallet.R.color.light_gray
import com.tari.android.wallet.R.dimen.common_copy_emoji_id_button_visible_bottom_margin
import com.tari.android.wallet.R.dimen.wallet_info_img_qr_code_size
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.ActivityWalletInfoBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.ui.activity.settings.SettingsActivity
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject

/**
 * Wallet info activity - show user emoji id and QR code view to share emoji ID.
 *
 * @author The Tari Development Team
 */
internal class WalletInfoActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var tracker: Tracker

    private lateinit var ui: ActivityWalletInfoBinding
    private lateinit var dimmerViews: List<View>
    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        ui = ActivityWalletInfoBinding.inflate(layoutInflater).apply { setContentView(root) }
        setupUi()
        tracker.screen(path = "/home/profile", title = "Profile - Wallet Info")
    }

    private fun setupUi() {
        dimmerViews =
            listOf(ui.headerDimmerView, ui.scrollDimmerView, ui.qrDimmerView, ui.bottomDimmerView)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        val emojiId = sharedPrefsWrapper.emojiId!!
        emojiIdSummaryController.display(emojiId)
        emojiIdCopiedViewController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
        // title
        val styledTitle = string(wallet_info_share_your_emoji_id).applyFontStyle(
            this,
            CustomFont.AVENIR_LT_STD_LIGHT,
            string(wallet_info_share_your_emoji_id_bold_part),
            CustomFont.AVENIR_LT_STD_BLACK,
            applyToOnlyFirstOccurence = true
        )
        ui.shareEmojiIdTextView.text = styledTitle

        val content = WalletUtil.getEmojiIdDeepLink(emojiId)
        UiUtil.getQREncodedBitmap(content, dimenPx(wallet_info_img_qr_code_size))?.let {
            ui.qrImageView.setImageBitmap(it)
        }

        ui.fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            emojiId,
            string(emoji_id_chunk_separator),
            color(black),
            color(light_gray)
        )

        ui.emojiIdSummaryContainerView.setOnClickListener(this::onEmojiSummaryClicked)
        ui.copyEmojiIdButton.setOnClickListener(this::onCopyEmojiIdButtonClicked)
        ui.copyEmojiIdButton.setOnLongClickListener { view ->
            onCopyEmojiIdButtonLongClicked(view)
            true
        }
        ui.closeButton.setOnClickListener { this.onCloseButtonClick() }
        ui.openSettingsCtaView.setOnClickListener(ThrottleClick { SettingsActivity.launch(this) })
        dimmerViews.forEach { it.setOnClickListener { this.hideFullEmojiId() } }
    }

    private fun onEmojiSummaryClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
        // make dimmers non-clickable until the anim is over
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        // prepare views
        ui.emojiIdSummaryContainerView.invisible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }
        val fullEmojiIdInitialWidth = ui.emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = ui.emojiIdContainerView.width - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            ui.fullEmojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        ui.fullEmojiIdContainerView.alpha = 0f
        ui.fullEmojiIdContainerView.visible()
        // scroll to end
        ui.fullEmojiIdScrollView.post {
            ui.fullEmojiIdScrollView.scrollTo(
                ui.fullEmojiIdTextView.width - ui.fullEmojiIdScrollView.width,
                0
            )
        }
        ui.copyEmojiIdContainerView.alpha = 0f
        ui.copyEmojiIdContainerView.visible()
        ui.copyEmojiIdContainerView.translationY = 0F
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // display overlay dimmers
            dimmerViews.forEach { it.alpha = value * 0.6f }
//             container alpha & scale
            ui.fullEmojiIdContainerView.alpha = value
            ui.fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            ui.fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            ui.copyEmojiIdContainerView.alpha = value
            ui.copyEmojiIdContainerView.translationY =
                -dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        copyEmojiIdButtonAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAnim, copyEmojiIdButtonAnim)
        animSet.start()
        animSet.addListener(onEnd = { dimmerViews.forEach { it.isClickable = true } })
        // scroll animation
        ui.fullEmojiIdScrollView.postDelayed({
            ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true) {
        dimmerViews.forEach { UiUtil.temporarilyDisableClick(it) }
        ui.fullEmojiIdScrollView.smoothScrollTo(0, 0)
        ui.emojiIdSummaryContainerView.visible()
        // copy emoji id button anim
        // emoji id anim
        val fullEmojiIdInitialWidth = ui.emojiIdContainerView.width
        val fullEmojiIdDeltaWidth =
            ui.emojiIdSummaryContainerView.width - ui.emojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // hide overlay dimmers
            dimmerViews.forEach { it.alpha = (1 - value) * 0.6f }
            // container alpha & scale
            ui.fullEmojiIdContainerView.alpha = 1 - value
            UiUtil.setWidth(
                ui.fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        // chain anim.s and start
        val animSet = AnimatorSet()
        if (animateCopyEmojiIdButton) {
            val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f).apply {
                addUpdateListener {
                    val value = it.animatedValue as Float
                    ui.copyEmojiIdContainerView.alpha = value
                    ui.copyEmojiIdContainerView.translationY =
                        -dimenPx(common_copy_emoji_id_button_visible_bottom_margin) * value
                }
            }
            animSet.playSequentially(copyEmojiIdButtonAnim, emojiIdAnim)
        } else {
            animSet.play(emojiIdAnim)
        }
        animSet.start()
        animSet.addListener(onEnd = {
            dimmerViews.forEach(View::gone)
            ui.fullEmojiIdContainerView.gone()
            ui.copyEmojiIdContainerView.gone()
        })
    }

    private fun completeCopy(clipboardString: String) {
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        val clipBoard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Identity",
            clipboardString
        )
        clipBoard?.setPrimaryClip(clipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false)
        }
        val copyEmojiIdButtonAnim = ui.copyEmojiIdContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    private fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopy(sharedPrefsWrapper.emojiId!!)
    }

    private fun onCopyEmojiIdButtonLongClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        completeCopy(sharedPrefsWrapper.publicKeyHexString!!)
    }

    private fun onCloseButtonClick() {
        finish()
    }

}
