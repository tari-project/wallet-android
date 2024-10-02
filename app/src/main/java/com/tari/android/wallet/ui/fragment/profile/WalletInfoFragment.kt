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
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.makeTextBold
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.component.tari.toolbar.TariToolbarActionArg
import com.tari.android.wallet.ui.extension.drawable
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

class WalletInfoFragment : CommonFragment<FragmentWalletInfoBinding, WalletInfoViewModel>() {

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
        collectFlow(uiState) { uiState ->
            ui.emojiIdSummaryView.textViewEmojiPrefix.text = uiState.walletAddress.addressPrefixEmojis()
            ui.emojiIdSummaryView.textViewEmojiFirstPart.text = uiState.walletAddress.addressFirstEmojis()
            ui.emojiIdSummaryView.textViewEmojiLastPart.text = uiState.walletAddress.addressLastEmojis()

            ui.alias.setVisible(uiState.alias.isNotBlank())
            ui.alias.text = uiState.alias

            ui.yatButton.setVisible(!uiState.yat.isNullOrBlank() && DebugConfig.isYatEnabled)
            ui.yatButton.setOnClickListener { viewModel.onYatButtonClicked() }
            ui.yatButton.setImageDrawable(drawable(if (uiState.yatShowing) R.drawable.vector_tari_yat_open else R.drawable.vector_tari_yat_close))
            ui.yatDisconnectedDescTextView.setVisible(uiState.yatDisconnected && DebugConfig.isYatEnabled)
            ui.yatDisconnectedDescTextView.text = string(R.string.wallet_info_yat_disconnected_description)
                .makeTextBold(requireContext(), string(R.string.wallet_info_yat_disconnected_description_connect_yats))
            ui.emojiIdAddressText.setVisible(!uiState.yatShowing || !DebugConfig.isYatEnabled, View.INVISIBLE)
            ui.yatAddressText.setVisible(uiState.yatShowing && DebugConfig.isYatEnabled)
            ui.yatAddressText.text = uiState.yat.orEmpty()
        }
    }

    private fun setupUI() {
        ui.emojiIdSummaryContainerView.setOnClickListener { viewModel.onAddressDetailsClicked() }

        ui.roundButtonsContainer.addView(RoundButtonWithIconView(requireContext()).apply {
            setArgs(
                text = string(R.string.wallet_info_wallet_button),
                icon = R.drawable.vector_wallet_wallet,
                action = { viewModel.onOpenWalletClicked() },
            )
        })
        if (DebugConfig.isYatEnabled) {
            ui.roundButtonsContainer.addView(RoundButtonWithIconView(requireContext()).apply {
                setArgs(
                    text = string(R.string.wallet_info_connect_yat_button),
                    icon = R.drawable.vector_wallet_yat,
                    action = { viewModel.openYatOnboarding(requireContext()) },
                )
            })
        }

        ui.toolbar.setRightArgs(TariToolbarActionArg(title = string(R.string.tx_detail_edit)) { viewModel.showEditAliasDialog() })

        ui.buttonShareAddress.setOnClickListener { viewModel.onShareAddressClicked() }

        viewModel.getQrCodeBitmap()?.let { ui.qrImageView.setImageBitmap(it) }
    }
}
