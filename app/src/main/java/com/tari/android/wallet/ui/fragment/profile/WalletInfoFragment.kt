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
package com.tari.android.wallet.ui.fragment.profile

import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentWalletInfoBinding
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.EmojiUtil
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import java.util.*
import javax.inject.Inject

class WalletInfoFragment : Fragment() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var clipboardManager: ClipboardManager

    private lateinit var ui: FragmentWalletInfoBinding

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    // region Lifecycle
    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ui = FragmentWalletInfoBinding.inflate(inflater, container, false)
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)
        emojiIdSummaryController.display(sharedPrefsWrapper.emojiId!!)

        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            requireContext()
        )
        fullEmojiIdViewController.fullEmojiId = sharedPrefsWrapper.emojiId!!
        fullEmojiIdViewController.emojiIdHex = sharedPrefsWrapper.publicKeyHexString!!

        ui.root.doOnGlobalLayout {
            ui.emojiIdOuterContainer.fullEmojiIdContainerView.apply {
                setTopMargin(ui.emojiIdSummaryContainerView.top)
                setLayoutHeight(ui.emojiIdSummaryContainerView.height)
                setLayoutWidth(ui.emojiIdSummaryContainerView.width)
            }
        }

        return ui.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    // endregion Lifecycle

    // region Initial UI Setup
    private fun setupUI() {
        val emojiId = sharedPrefsWrapper.emojiId!!
        displayQRCode(emojiId)
        setupCTAs()
    }

    private fun displayQRCode(emojiId: String) {
        val content = WalletUtil.getEmojiIdDeepLink(emojiId)
        getQREncodedBitmap(content, dimenPx(R.dimen.wallet_info_img_qr_code_size))?.let {
            ui.qrImageView.setImageBitmap(it)
        }
    }

    private fun getQREncodedBitmap(content: String, size: Int): Bitmap? {
        try {
            val barcodeEncoder = BarcodeEncoder()
            val hints: MutableMap<EncodeHintType, String> =
                EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            return barcodeEncoder.createBitmap(map)
        } catch (e: Exception) {
        }
        return null
    }

    // endregion Initial UI Setup

    // region CTAs setup

    private fun setupCTAs() {
        ui.emojiIdSummaryContainerView.setOnClickListener(this::onEmojiSummaryClicked)
    }

    private fun onEmojiSummaryClicked(view: View) {
        view.temporarilyDisableClick()
        fullEmojiIdViewController.showFullEmojiId()
    }

    // endregion CTAs setup

}
