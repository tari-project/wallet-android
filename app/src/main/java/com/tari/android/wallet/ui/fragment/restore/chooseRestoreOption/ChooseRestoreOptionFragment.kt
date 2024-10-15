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
package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.fragment.app.viewModels
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.databinding.FragmentChooseRestoreOptionBinding
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.parcelable
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.qr.QrScannerActivity
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.option.RecoveryOptionView
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOption
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto

class ChooseRestoreOptionFragment : CommonFragment<FragmentChooseRestoreOptionBinding, ChooseRestoreOptionViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentChooseRestoreOptionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: ChooseRestoreOptionViewModel by viewModels()
        bindViewModel(viewModel)

        setupUI()

        observeUI()
    }

    override fun onResume() {
        super.onResume()
        onActivityResult(0, 0, null)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == QrScannerActivity.REQUEST_QR_SCANNER && resultCode == Activity.RESULT_OK && data != null) {
            val qrDeepLink = data.parcelable<DeepLink>(QrScannerActivity.EXTRA_DEEPLINK) ?: return
            viewModel.handleDeeplink(qrDeepLink)
        } else {
            viewModel.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupUI() = with(ui) {
        restoreWithRecoveryPhraseCtaView.setOnClickListener { viewModel.onRecoveryPhraseClicked() }
        restoreWithPaperWalletCtaView.setOnClickListener { viewModel.onPaperWalletClicked(this@ChooseRestoreOptionFragment) }
    }

    private fun observeUI() = with(viewModel) {
        collectFlow(uiState) { uiState ->
            initOptions(uiState.backupOptions)
            uiState.selectedOption?.let { updateProgress(it, uiState.isStarted) }

            if (uiState.paperWalletProgress) {
                ui.restoreWithPaperWalletCtaView.isEnabled = false
                ui.restoreWithPaperWalletProgressView.visible()
                ui.restoreWithPaperWalletArrow.gone()
            } else {
                ui.restoreWithPaperWalletCtaView.isEnabled = true
                ui.restoreWithPaperWalletProgressView.gone()
                ui.restoreWithPaperWalletArrow.visible()
            }
        }
    }

    private fun initOptions(options: List<BackupOptionDto>) {
        ui.optionsContainer.removeAllViews()
        for (option in options) {
            val view = RecoveryOptionView(requireContext()).apply {
                viewLifecycle = viewLifecycleOwner
                ui.restoreWalletCtaView.setOnClickListener {
                    this@ChooseRestoreOptionFragment.viewModel.startRecovery(
                        selectedOption = option.type,
                        hostFragment = this@ChooseRestoreOptionFragment,
                    )
                }
                init(option.type)
            }
            ui.optionsContainer.addView(view)
        }
    }

    private fun updateProgress(backupOption: BackupOption, isStarted: Boolean) {
        blockingBackPressDispatcher.isEnabled = isStarted
        getBackupOptionView(backupOption)?.updateLoading(isStarted)
    }

    private fun getBackupOptionView(backupOptions: BackupOption): RecoveryOptionView? =
        ui.optionsContainer.children.mapNotNull { it as? RecoveryOptionView }.firstOrNull { it.viewModel.option == backupOptions }
}

