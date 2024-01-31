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
package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tari.android.wallet.databinding.FragmentWalletBackupSettingsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.ThrottleClick
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionView
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel

class BackupSettingsFragment : CommonFragment<FragmentWalletBackupSettingsBinding, BackupSettingsViewModel>() {

    private val biometricAuthenticationViewModel: BiometricAuthenticationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentWalletBackupSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: BackupSettingsViewModel by viewModels()
        bindViewModel(viewModel)

        BiometricAuthenticationViewModel.bindToFragment(biometricAuthenticationViewModel, this)
        viewModel.biometricAuthenticationViewModel = biometricAuthenticationViewModel

        setupCTAs()
        subscribeUI()
    }

    override fun onResume() {
        super.onResume()
        onActivityResult(0, 0, null)
        viewModel.backupStateChanged.postValue(Unit)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setSeedWordVerificationStateIcon()
    }

    private fun setupCTAs() = with(ui) {
        backupWithRecoveryPhraseCtaView.setOnClickListener(ThrottleClick { viewModel.onBackupWithRecoveryPhrase() })
        backupWalletToCloudCtaView.setOnClickListener(ThrottleClick { viewModel.onBackupToCloud() })
        updatePasswordCtaView.setOnClickListener(ThrottleClick { viewModel.onUpdatePassword() })
        learnMoreCtaView.setOnClickListener(ThrottleClick { viewModel.learnMore() })
    }

    private fun subscribeUI() = with(viewModel) {
        observe(backupStateChanged) { resetStatusIcons() }

        observe(isBackupNowAvailable) { ui.backupWalletToCloudCtaContainerView.setVisible(it) }

        observe(setPasswordVisible) { ui.updatePasswordCtaView.setVisible(it) }

        observe(optionViewModels) { initBackupOptions(it) }
    }

    private fun initBackupOptions(options: List<BackupOptionViewModel>) {
        for (option in options) {
            val backupOptionView = BackupOptionView(requireContext())
            backupOptionView.viewLifecycle = viewLifecycleOwner
            backupOptionView.init(this, option)
            ui.optionsContainer.addView(backupOptionView)
        }
    }

    private fun resetStatusIcons() {
        setSeedWordVerificationStateIcon()
    }

    private fun setSeedWordVerificationStateIcon() = with(ui) {
        val hasVerifiedSeedWords = viewModel.tariSettingsSharedRepository.hasVerifiedSeedWords
        backupWithRecoveryPhraseSuccessView.setVisible(hasVerifiedSeedWords)
        backupWithRecoveryPhraseWarningView.setVisible(!hasVerifiedSeedWords)
    }

    companion object {
        fun newInstance() = BackupSettingsFragment()
    }
}