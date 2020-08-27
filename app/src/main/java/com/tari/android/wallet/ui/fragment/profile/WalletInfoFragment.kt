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

import android.animation.Animator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.daasuu.ei.Ease
import com.daasuu.ei.EasingInterpolator
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tari.android.wallet.R.dimen.common_copy_emoji_id_button_visible_bottom_margin
import com.tari.android.wallet.R.dimen.wallet_info_img_qr_code_size
import com.tari.android.wallet.databinding.FragmentWalletInfoBinding
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.model.yat.EmojiSet
import com.tari.android.wallet.ui.component.EmojiIdCopiedViewController
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import com.tari.android.wallet.util.WalletUtil
import java.util.*
import javax.inject.Inject

class WalletInfoFragment : Fragment() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsWrapper

    @Inject
    lateinit var userStorage: YatUserStorage

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var set: EmojiSet

    private lateinit var summaryController: EmojiIdSummaryViewController
    private lateinit var copyController: EmojiIdCopiedViewController
    private lateinit var ui: FragmentWalletInfoBinding

    @Suppress("unused")
    private val FragmentWalletInfoBinding.dimmerViews
        get() = arrayOf(
            ui.scrollDimmerView,
            ui.qrDimmerView,
            ui.bottomDimmerView,
        )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentWalletInfoBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        copyController = EmojiIdCopiedViewController(ui.emojiIdCopiedView)
        summaryController = EmojiIdSummaryViewController(ui.emojiIdSummaryView, set)
        displayUserYat()
        ui.copyEmojiIdButton.isEnabled = false
        setupCTAs()
    }

    private fun setupCTAs() {
        ui.emojiIdSummaryContainerView.setOnClickListener { animateCopyButtonAppearance() }
        ui.dimmerViews.forEach {
            it.setOnClickListener {
                ui.copyEmojiIdButton.isEnabled = false
                ui.dimmerViews.forEach { dimmer -> dimmer.isClickable = false }
                brightenPage { brighteningAnimation ->
                    playSequentially(createCopyButtonAnimation(), brighteningAnimation)
                }
            }
        }
        ui.copyEmojiIdButton.setOnClickListener {
            copyYat()
            animateCopyAndHideCopyRelatedLayout()
        }
        ui.copyEmojiIdButton.setOnLongClickListener {
            copyHex()
            animateCopyAndHideCopyRelatedLayout()
            true
        }
    }

    private fun displayUserYat() {
        val yat = userStorage.get()!!.emojiIds.first()
        summaryController.display(yat.raw)
        val content =
            WalletUtil.getPublicKeyHexDeepLink(sharedPrefsWrapper.publicKeyHexString!!, yat)
        displayQRCode(content)
    }

    private fun animateCopyButtonAppearance() {
        ui.emojiIdSummaryContainerView.setOnClickListener(null)
        val dimPage = animateValues(
            values = floatArrayOf(0F, 0.6F),
            duration = Constants.UI.shortDurationMs,
            onUpdate = {
                val alpha = it.animatedValue as Float
                ui.dimmerViews.forEach { view -> view.alpha = alpha }
            }
        )
        val showCopyButton = animatorSetOf(
            duration = Constants.UI.shortDurationMs,
            children = playTogether(
                animateValues(
                    values = floatArrayOf(0F, 1F),
                    onUpdate = {
                        ui.copyEmojiIdButtonContainerView.alpha = it.animatedValue as Float
                    }
                ),
                animateValues(
                    values = floatArrayOf(1F, 0F),
                    interpolator = EasingInterpolator(Ease.BACK_OUT),
                    onUpdate = {
                        ui.copyEmojiIdButtonContainerView.translationY =
                            dimenPx(common_copy_emoji_id_button_visible_bottom_margin) *
                                    it.animatedValue as Float
                    }
                ),
            )
        )
        animatorSetOf(
            onStart = {
                ui.dimmerViews.forEach {
                    it.isClickable = false
                    it.alpha = 0f
                    it.visible()
                }
                ui.copyEmojiIdButtonContainerView.alpha = 0f
                ui.copyEmojiIdButtonContainerView.visible()
            },
            onEnd = {
                ui.copyEmojiIdButton.isEnabled = true
                ui.dimmerViews.forEach { it.isClickable = true }
            },
            children = playSequentially(dimPage, showCopyButton),
        ).start()
    }

    private fun brightenPage(animationStrategyFactory: (Animator) -> AnimatorSetPlayStrategy) {
        animatorSetOf(
            onEnd = {
                ui.emojiIdSummaryContainerView.setOnClickListener { animateCopyButtonAppearance() }
                ui.dimmerViews.forEach(View::gone)
            },
            children = animationStrategyFactory(
                animateValues(
                    values = floatArrayOf(0.6F, 0F),
                    onUpdate = {
                        val alpha = it.animatedValue as Float
                        ui.dimmerViews.forEach { view -> view.alpha = alpha }
                    }
                ),
            ),
        ).start()
    }

    private fun createCopyButtonAnimation() = animateValues(
        duration = Constants.UI.shortDurationMs,
        values = floatArrayOf(0F, 1F),
        onUpdate = {
            ui.copyEmojiIdButtonContainerView.alpha = 1F - it.animatedValue as Float
            ui.copyEmojiIdButtonContainerView.translationY =
                dimenPx(common_copy_emoji_id_button_visible_bottom_margin) *
                        it.animatedValue as Float
        }
    )

    private fun animateCopyAndHideCopyRelatedLayout() {
        ui.copyEmojiIdButton.isEnabled = false
        ui.dimmerViews.forEach { it.isClickable = false }
        copyController.showEmojiIdCopiedAnim(fadeOutOnEnd = true) { brightenPage(::playOnly) }
        animateValues(
            values = floatArrayOf(1F, 0F),
            duration = Constants.UI.xShortDurationMs,
            onUpdate = {
                ui.copyEmojiIdButtonContainerView.alpha = it.animatedValue as Float
            }
        ).start()
    }

    private fun copyYat() = copyToClipboard(userStorage.get()!!.emojiIds.first().raw)

    private fun copyHex() = copyToClipboard(sharedPrefsWrapper.publicKeyHexString!!)

    private fun copyToClipboard(content: String) =
        clipboardManager.setPrimaryClip(ClipData.newPlainText("Tari Wallet Identity", content))

    private fun displayQRCode(content: String) {
        encodeToQR(content, dimenPx(wallet_info_img_qr_code_size))
            ?.let(ui.qrImageView::setImageBitmap)
    }

    private fun encodeToQR(content: String, size: Int): Bitmap? = try {
        val barcodeEncoder = BarcodeEncoder()
        val hints = EnumMap<EncodeHintType, String>(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        barcodeEncoder.createBitmap(map)
    } catch (e: Exception) {
        null
    }

}
