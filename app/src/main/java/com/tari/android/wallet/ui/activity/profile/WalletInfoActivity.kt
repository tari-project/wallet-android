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

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import butterknife.*
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import me.everything.android.ui.overscroll.OverScrollDecoratorHelper
import org.matomo.sdk.Tracker
import org.matomo.sdk.extra.TrackHelper
import javax.inject.Inject

/**
 * Wallet info activity - show user emoji id and QR code view to share emoji ID.
 *
 * @author The Tari Development Team
 */
internal class WalletInfoActivity : BaseActivity() {

    @BindView(R.id.wallet_info_scroll_emoji_id)
    lateinit var emojiIdScrollView: HorizontalScrollView
    @BindView(R.id.wallet_info_txt_share_emoji_id)
    lateinit var shareEmojiIdTextView: TextView
    @BindView(R.id.wallet_info_txt_emoji_id)
    lateinit var emojiIdTextView: TextView
    @BindView(R.id.wallet_info_img_qr)
    lateinit var qrCodeImageView: ImageView
    @BindView(R.id.wallet_info_txt_copy_emoji_id)
    lateinit var copyEmojiIdTextView: TextView
    @BindView(R.id.wallet_info_vw_emoji_id_copied)
    lateinit var emojiIdCopiedAnimView: View

    @BindString(R.string.wallet_info_share_your_emoji_id)
    lateinit var shareEmojiIdTitle: String
    @BindString(R.string.wallet_info_share_your_emoji_id_bold_part)
    lateinit var shareEmojiIdTitleBoldPart: String
    @BindString(R.string.emoji_id_chunk_separator_char)
    lateinit var emojiIdChunkSeparator: String

    @BindDimen(R.dimen.wallet_info_img_qr_code_size)
    @JvmField
    var qrCodeImageSize = 0

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper
    @Inject
    lateinit var tracker: Tracker

    /**
     * Animates the emoji id "copied" text.
     */
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
        // title
        val styledTitle = shareEmojiIdTitle.applyFontStyle(
            this,
            CustomFont.AVENIR_LT_STD_LIGHT,
            shareEmojiIdTitleBoldPart,
            CustomFont.AVENIR_LT_STD_BLACK,
            applyToOnlyFirstOccurence = true
        )
        shareEmojiIdTextView.text = styledTitle

        val chunkedEmojiId =
            EmojiUtil.getChunkedEmojiId(sharedPrefsWrapper.emojiId!!, emojiIdChunkSeparator)
        emojiIdTextView.text = chunkedEmojiId

        val content = WalletUtil.getEmojiIdDeepLink(sharedPrefsWrapper.emojiId!!)
        UiUtil.getQREncodedBitmap(content, qrCodeImageSize)?.let {
            qrCodeImageView.setImageBitmap(it)
        }

        OverScrollDecoratorHelper.setUpOverScroll(emojiIdScrollView)

        emojiIdCopiedViewController = EmojiIdCopiedViewController(emojiIdCopiedAnimView)
    }

    @OnClick(R.id.wallet_info_btn_close)
    fun onCloseButtonClick() {
        finish()
    }

    /*
    * Copy user emoji id to the user's clipboard
    */
    @OnClick(R.id.wallet_info_txt_copy_emoji_id)
    fun onCopyEmojiIdClick() {
        UiUtil.temporarilyDisableClick(copyEmojiIdTextView)
        val clipBoard = ContextCompat.getSystemService(this, ClipboardManager::class.java)
        val deepLinkClipboardData = ClipData.newPlainText(
            "Tari Wallet Emoji Id",
            EmojiUtil.getChunkedEmojiId(sharedPrefsWrapper.emojiId!!, emojiIdChunkSeparator)
        )
        clipBoard?.setPrimaryClip(deepLinkClipboardData)
        emojiIdCopiedViewController.showEmojiIdCopiedAnim(fadeOutOnEnd = true)
    }

}