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

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentWalletInfoBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.FullEmojiIdViewController
import com.tari.android.wallet.ui.extension.*
import java.util.*

class WalletInfoFragment : CommonFragment<FragmentWalletInfoBinding, WalletInfoViewModel>() {

    private lateinit var emojiIdSummaryController: EmojiIdSummaryViewController
    private lateinit var fullEmojiIdViewController: FullEmojiIdViewController

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        ui = FragmentWalletInfoBinding.inflate(inflater, container, false)

        val viewModel: WalletInfoViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()
        subscribeUI()

        return ui.root
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshData()
    }

    private fun subscribeUI() = with(viewModel) {
        observe(emojiId) {
            emojiIdSummaryController.display(it)
            fullEmojiIdViewController.fullEmojiId = it
        }

        observe(publicKeyHex) { fullEmojiIdViewController.emojiIdHex = it }

        observe(qrDeepLink) {
            getQREncodedBitmap(it, dimenPx(R.dimen.wallet_info_img_qr_code_size))?.let { bitmap ->
                ui.qrImageView.setImageBitmap(bitmap)
            }
        }

        observe(yat) { ui.yatButton.setVisible(it.isNotEmpty()) }

        observe(isYatForegrounded) {
            val icon = if (it) R.drawable.tari_yat_open else R.drawable.tari_yat_close
            val drawable = ContextCompat.getDrawable(requireContext(), icon)
            ui.yatButton.setImageDrawable(drawable)
        }

        observe(reconnectVisibility) {
            val text = if (it) R.string.wallet_info_yat_disconnected_description else R.string.wallet_info_qr_code_desc
            ui.descTextView.setText(text)
            ui.reconnectButton.setVisible(it)
        }

        observeOnLoad(yatDisconnected)
    }

    private fun setupUI() {
        ui.emojiIdSummaryContainerView.setOnClickListener(this::onEmojiSummaryClicked)

        emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView)

        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryView,
            requireContext()
        )

        ui.root.doOnGlobalLayout {
            ui.emojiIdOuterContainer.fullEmojiIdContainerView.apply {
                setTopMargin(ui.emojiIdSummaryContainerView.top)
                setLayoutHeight(ui.emojiIdSummaryContainerView.height)
                setLayoutWidth(ui.emojiIdSummaryContainerView.width)
            }
        }

        ui.yatButton.setOnClickListener { viewModel.changeYatVisibility() }

        ui.reconnectButton.setOnThrottledClickListener { viewModel.openYatOnboarding(requireContext()) }
    }

    private fun getQREncodedBitmap(content: String, size: Int): Bitmap? {
        return try {
            val hints: MutableMap<EncodeHintType, String> = EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val barcodeEncoder = BarcodeEncoder()
            val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            barcodeEncoder.createBitmap(map)
        } catch (e: Throwable) {
            null
        }
    }

    private fun onEmojiSummaryClicked(view: View) {
        view.temporarilyDisableClick()
        fullEmojiIdViewController.showFullEmojiId()
    }
}

