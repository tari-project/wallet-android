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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.FragmentWalletInfoBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.extension.observeOnLoad
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdWithYatSummaryViewController
import com.tari.android.wallet.ui.component.fullEmojiId.FullEmojiIdViewController
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.doOnGlobalLayout
import com.tari.android.wallet.ui.extension.setLayoutHeight
import com.tari.android.wallet.ui.extension.setLayoutWidth
import com.tari.android.wallet.ui.extension.setOnThrottledClickListener
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareOptionView
import com.tari.android.wallet.ui.fragment.contact_book.root.share.ShareType
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation

class WalletInfoFragment : CommonFragment<FragmentWalletInfoBinding, WalletInfoViewModel>() {

    private lateinit var emojiIdSummaryController: EmojiIdWithYatSummaryViewController
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
            emojiIdSummaryController.emojiId = it
            fullEmojiIdViewController.fullEmojiId = it
        }

        observe(publicKeyHex) { fullEmojiIdViewController.emojiIdHex = it }

        observe(yat) { emojiIdSummaryController.yat = it }

        observe(reconnectVisibility) {
            val text = if (it) R.string.wallet_info_yat_disconnected_description else R.string.wallet_info_qr_code_desc
            ui.descTextView.setText(text)
            ui.reconnectButton.setVisible(it)
        }

        observeOnLoad(yatDisconnected)

        observe(alias) { updateAlias(it) }
    }

    private fun setupUI() {
        ui.emojiIdSummaryWithYatView.emojiIdSummaryContainerView.setOnClickListener(this::onEmojiSummaryClicked)

        val qrCodeArgs = ShareOptionArgs(ShareType.QR_CODE, string(R.string.share_contact_via_qr_code), R.drawable.vector_share_qr_code) {
            viewModel.shareData(ShareType.QR_CODE)
        }

        val linkArgs = ShareOptionArgs(ShareType.LINK, string(R.string.share_contact_via_qr_link), R.drawable.vector_share_link) {
            viewModel.shareData(ShareType.LINK)
        }
        val bleArgs = ShareOptionArgs(ShareType.BLE, string(R.string.share_contact_via_qr_ble), R.drawable.vector_share_ble) {
            viewModel.shareData(ShareType.BLE)
        }

        ui.shareTypeFirstRow.addView(ShareOptionView(requireContext()).apply { setArgs(qrCodeArgs, ShareOptionView.Size.Medium) })
        ui.shareTypeFirstRow.addView(ShareOptionView(requireContext()).apply { setArgs(linkArgs, ShareOptionView.Size.Medium) })
        ui.shareTypeFirstRow.addView(ShareOptionView(requireContext()).apply { setArgs(bleArgs, ShareOptionView.Size.Medium) })

        ui.auroraContainer.addView(RoundButtonWithIconView(requireContext()).apply {
            setArgs(getString(R.string.wallet_info_wallet_button), R.drawable.vector_wallet_wallet, {
                viewModel.navigation.postValue(Navigation.TxListNavigation.ToUtxos)
            })
        })
        ui.auroraContainer.addView(RoundButtonWithIconView(requireContext()).apply {
            setArgs(getString(R.string.wallet_info_connect_yat_button), R.drawable.vector_wallet_yat, {
                viewModel.yatAdapter.openOnboarding(requireContext())
            })
        })

        ui.toolbar.setRightArgs(TariToolbarActionArg(title = string(R.string.tx_detail_edit)) { viewModel.showEditAliasDialog() })

        emojiIdSummaryController = EmojiIdWithYatSummaryViewController(ui.emojiIdSummaryWithYatView)

        fullEmojiIdViewController = FullEmojiIdViewController(
            ui.emojiIdOuterContainer,
            ui.emojiIdSummaryWithYatView.emojiIdSummaryView,
            requireContext()
        )

        ui.root.doOnGlobalLayout {
            ui.emojiIdOuterContainer.fullEmojiIdContainerView.apply {
                setTopMargin(ui.emojiIdSummaryWithYatView.root.top)
                setLayoutHeight(ui.emojiIdSummaryWithYatView.root.height)
                setLayoutWidth(ui.emojiIdSummaryWithYatView.root.width)
            }
        }

        ui.reconnectButton.setOnThrottledClickListener { viewModel.openYatOnboarding(requireContext()) }

        ui.requestTari.setOnClickListener { viewModel.openRequestTari() }
    }

    private fun onEmojiSummaryClicked(view: View) {
        view.temporarilyDisableClick()
        fullEmojiIdViewController.showFullEmojiId()
    }

    private fun updateAlias(alias: String?) {
        ui.alias.setVisible(alias.orEmpty().isNotBlank())
        ui.alias.text = alias.orEmpty()
    }
}

