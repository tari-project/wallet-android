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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import butterknife.*
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.invisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import javax.inject.Inject

/**
 * Wallet info activity - show user emoji id and QR code view to share emoji ID.
 *
 * @author The Tari Development Team
 */
internal class WalletInfoActivity : BaseActivity() {

    @BindView(R.id.wallet_info_txt_share_emoji_id)
    lateinit var shareEmojiIdTextView: TextView
    @BindView(R.id.wallet_info_img_qr)
    lateinit var qrCodeImageView: ImageView

    @BindString(R.string.wallet_info_share_your_emoji_id)
    lateinit var shareEmojiIdTitle: String
    @BindString(R.string.wallet_info_share_your_emoji_id_bold_part)
    lateinit var shareEmojiIdTitleBoldPart: String
    @BindString(R.string.emoji_id_chunk_separator)
    lateinit var emojiIdChunkSeparator: String

    @BindView(R.id.wallet_info_emoji_id_container)
    lateinit var emojiIdContainerView: View

    @BindView(R.id.wallet_info_emoji_id_summary)
    lateinit var emojiIdSummaryView: View

    @BindView(R.id.wallet_info_emoji_id_copied)
    lateinit var emojiIdCopiedAnimView: View

    @BindView(R.id.wallet_info_full_emoji_id_container)
    lateinit var fullEmojiIdContainerView: View

    @BindView(R.id.wallet_info_scroll_full_emoji_id)
    lateinit var fullEmojiIdScrollView: HorizontalScrollView

    @BindView(R.id.wallet_info_txt_full_emoji_id)
    lateinit var fullEmojiIdTextView: TextView

    @BindView(R.id.wallet_info_emoji_id_summary_container)
    lateinit var emojiIdSummaryContainerView: View

    @BindView(R.id.wallet_info_copy_emoji_id_container)
    lateinit var copyEmojiIdButtonContainerView: View

    /**
     * Dimmers.
     */
    @BindViews(
        R.id.wallet_info_header_dimmer,
        R.id.wallet_info_scroll_dimmer,
        R.id.wallet_info_underscroll_dimmer,
        R.id.wallet_info_qr_code_dimmer
    )
    lateinit var dimmerViews: List<@JvmSuppressWildcards View>

    @BindColor(R.color.black)
    @JvmField
    var blackColor = 0
    @BindColor(R.color.light_gray)
    @JvmField
    var lightGrayColor = 0

    @BindDimen(R.dimen.wallet_info_img_qr_code_size)
    @JvmField
    var qrCodeImageSize = 0

    @BindDimen(R.dimen.common_copy_emoji_id_button_visible_bottom_margin)
    @JvmField
    var copyEmojiIdButtonVisibleBottomMargin = 0

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var tracker: Tracker

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private lateinit var emojiIdCopiedViewController: EmojiIdCopiedViewController

    override val contentViewId = R.layout.activity_wallet_info

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpUi()

        TrackHelper.track()
            .screen("/home/profile")
            .title("Profile - Wallet Info")
            .with(tracker)
    }

    private fun setUpUi() {
        val emojiId = sharedPrefsWrapper.emojiId!!
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiIdSummaryView)
        emojiIdSummaryController.display(emojiId)
        emojiIdCopiedViewController = EmojiIdCopiedViewController(emojiIdCopiedAnimView)
        // title
        val styledTitle = shareEmojiIdTitle.applyFontStyle(
            this,
            CustomFont.AVENIR_LT_STD_LIGHT,
            shareEmojiIdTitleBoldPart,
            CustomFont.AVENIR_LT_STD_BLACK,
            applyToOnlyFirstOccurence = true
        )
        shareEmojiIdTextView.text = styledTitle

        val content = WalletUtil.getEmojiIdDeepLink(emojiId)
        UiUtil.getQREncodedBitmap(content, qrCodeImageSize)?.let {
            qrCodeImageView.setImageBitmap(it)
        }

        fullEmojiIdTextView.text = EmojiUtil.getFullEmojiIdSpannable(
            emojiId,
            emojiIdChunkSeparator,
            blackColor,
            lightGrayColor
        )
    }

    @OnClick(R.id.wallet_info_emoji_id_summary_container)
    fun onEmojiSummaryClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        showFullEmojiId()
    }

    private fun showFullEmojiId() {
//         make dimmers non-clickable until the anim is over
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        // prepare views
        emojiIdSummaryContainerView.invisible()
        dimmerViews.forEach { dimmerView ->
            dimmerView.alpha = 0f
            dimmerView.visible()
        }
        val fullEmojiIdInitialWidth = emojiIdSummaryContainerView.width
        val fullEmojiIdDeltaWidth = emojiIdContainerView.width - fullEmojiIdInitialWidth
        UiUtil.setWidth(
            fullEmojiIdContainerView,
            fullEmojiIdInitialWidth
        )
        fullEmojiIdContainerView.alpha = 0f
        fullEmojiIdContainerView.visible()
        // scroll to end
        fullEmojiIdScrollView.post {
            fullEmojiIdScrollView.scrollTo(
                fullEmojiIdTextView.width - fullEmojiIdScrollView.width,
                0
            )
        }
        // TODO
        copyEmojiIdButtonContainerView.alpha = 0f
        copyEmojiIdButtonContainerView.visible()
        copyEmojiIdButtonContainerView.translationY = 0F
        // animate full emoji id view
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // display overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = value * 0.6f
            }
//             container alpha & scale
            fullEmojiIdContainerView.alpha = value
            fullEmojiIdContainerView.scaleX = 1f + 0.2f * (1f - value)
            fullEmojiIdContainerView.scaleY = 1f + 0.2f * (1f - value)
            UiUtil.setWidth(
                fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        emojiIdAnim.duration = Constants.UI.shortDurationMs
        // copy emoji id button anim
        //TODO
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(0f, 1f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            copyEmojiIdButtonContainerView.alpha = value
            copyEmojiIdButtonContainerView.translationY = -copyEmojiIdButtonVisibleBottomMargin * value
        }
        copyEmojiIdButtonAnim.duration = Constants.UI.shortDurationMs
        copyEmojiIdButtonAnim.interpolator = EasingInterpolator(Ease.BACK_OUT)

        // chain anim.s and start
        val animSet = AnimatorSet()
        animSet.playSequentially(emojiIdAnim , copyEmojiIdButtonAnim)
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dimmerViews.forEach { dimmerView -> dimmerView.isClickable = true }
            }
        })
        // scroll animation
        fullEmojiIdScrollView.postDelayed({
            fullEmojiIdScrollView.smoothScrollTo(0, 0)
        }, Constants.UI.shortDurationMs + 20)
    }

    private fun hideFullEmojiId(animateCopyEmojiIdButton: Boolean = true) {
        fullEmojiIdScrollView.smoothScrollTo(0, 0)
        emojiIdSummaryContainerView.visible()
        // copy emoji id button anim
        val copyEmojiIdButtonAnim = ValueAnimator.ofFloat(1f, 0f)
        copyEmojiIdButtonAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            copyEmojiIdButtonContainerView.alpha = value
            copyEmojiIdButtonContainerView.translationY = -copyEmojiIdButtonVisibleBottomMargin * value
        }
        // emoji id anim
        val fullEmojiIdInitialWidth = emojiIdContainerView.width
        val fullEmojiIdDeltaWidth = emojiIdSummaryContainerView.width - emojiIdContainerView.width
        val emojiIdAnim = ValueAnimator.ofFloat(0f, 1f)
        emojiIdAnim.addUpdateListener { valueAnimator: ValueAnimator ->
            val value = valueAnimator.animatedValue as Float
            // hide overlay dimmers
            dimmerViews.forEach { dimmerView ->
                dimmerView.alpha = (1 - value) * 0.6f
            }
            // container alpha & scale
            fullEmojiIdContainerView.alpha = (1 - value)
            UiUtil.setWidth(
                fullEmojiIdContainerView,
                (fullEmojiIdInitialWidth + fullEmojiIdDeltaWidth * value).toInt()
            )
        }
        // chain anim.s and start
        val animSet = AnimatorSet()
        if (animateCopyEmojiIdButton) {
            animSet.playSequentially(copyEmojiIdButtonAnim, emojiIdAnim)
        } else {
            animSet.play(emojiIdAnim)
        }
        animSet.start()
        animSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                dimmerViews.forEach { dimmerView ->
                    dimmerView.gone()
                }
                fullEmojiIdContainerView.gone()
                copyEmojiIdButtonContainerView.gone()
            }
        })
    }

    /**
     * Dimmer clicked - hide dimmers.
     */
    @OnClick(
        R.id.wallet_info_header_dimmer,
        R.id.wallet_info_scroll_dimmer,
        R.id.wallet_info_underscroll_dimmer,
        R.id.wallet_info_qr_code_dimmer
    )
    fun onDimmerViewsClicked() {
        hideFullEmojiId()
    }

    @OnClick(R.id.wallet_info_btn_copy_emoji_id)
    fun onCopyEmojiIdButtonClicked(view: View) {
        UiUtil.temporarilyDisableClick(view)
        dimmerViews.forEach { dimmerView -> dimmerView.isClickable = false }
        val clipBoard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val clipboardData = ClipData.newPlainText(
            "Tari Wallet Emoji Id",
            sharedPrefsWrapper.emojiId!!
        )
        clipBoard?.setPrimaryClip(clipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) {
            hideFullEmojiId(animateCopyEmojiIdButton = false)
        }
        val copyEmojiIdButtonAnim = copyEmojiIdButtonContainerView.animate().alpha(0f)
        copyEmojiIdButtonAnim.duration = Constants.UI.xShortDurationMs
        copyEmojiIdButtonAnim.start()
    }

    @OnClick(R.id.wallet_info_btn_close)
    fun onCloseButtonClick() {
        finish()
    }

}
