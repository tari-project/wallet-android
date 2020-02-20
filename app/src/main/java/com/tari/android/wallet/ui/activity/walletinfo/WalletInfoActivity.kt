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
package com.tari.android.wallet.ui.activity.walletinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import butterknife.BindDimen
import butterknife.BindString
import butterknife.BindView
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ffi.FFIPublicKey
import com.tari.android.wallet.ui.activity.BaseActivity
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject
import javax.inject.Named

/**
 * Wallet info activity - show user emoji id and QR code view to share emoji ID.
 *
 * @author The Tari Development Team
 */
class WalletInfoActivity : BaseActivity() {

    @BindView(R.id.wallet_info_txt_emoji_container)
    lateinit var emojiContainerView: TextView
    @BindView(R.id.wallet_info_img_qr)
    lateinit var qrCodeImageView: ImageView
    @BindView(R.id.wallet_info_txt_copy_emoji_id)
    lateinit var copyEmojiIdTextView: TextView

    @BindString(R.string.emoji_id_chunk_separator_char)
    lateinit var emojiIdChunkSeparator: String
    @BindString(R.string.wallet_info_emoji_id_copied)
    lateinit var emojiIdCopiedToastMessage: String
    @BindDimen(R.dimen.wallet_info_img_qr_code_size)
    @JvmField
    var qrCodeImageSize = 0

    @Inject
    @Named(WalletModule.FieldName.emojiId)
    lateinit var emojiId: String

    override val contentViewId = R.layout.activity_wallet_info

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpUi()
    }

    private fun setUpUi() {
        val shortEmojiId = EmojiUtil.getShortenedEmojiId(emojiId)!!
        val chunkedEmojiId = EmojiUtil.getChunkedEmojiId(shortEmojiId, emojiIdChunkSeparator)
        emojiContainerView.text = chunkedEmojiId

        val content = WalletUtil.getQRContent(FFIPublicKey(emojiId).toString(), emojiId)
        UiUtil.getQREncodedBitmap(content, qrCodeImageSize)?.let {
            qrCodeImageView.setImageBitmap(it)
        }
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
        val clip = ClipData.newPlainText("EmojiId", emojiId)
        clipBoard?.setPrimaryClip(clip)
        Toast.makeText(this, emojiIdCopiedToastMessage, Toast.LENGTH_SHORT).show()
    }
}