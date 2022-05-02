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
package com.tari.android.wallet.ui.fragment.settings.allSettings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentAllSettingsBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.ui.activity.settings.BackupSettingsActivity
import com.tari.android.wallet.ui.activity.settings.DeleteWalletActivity
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.settings.backgroundService.BackgroundServiceSettingsActivity
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

internal class AllSettingsFragment : CommonFragment<FragmentAllSettingsBinding, AllSettingsViewModel>() {

    private val optionsAdapter = AllSettingsOptionAdapter()

    private val biometricAuthenticationViewModel: BiometricAuthenticationViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentAllSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AllSettingsViewModel by viewModels()
        bindViewModel(viewModel)

        BiometricAuthenticationViewModel.bindToFragment(biometricAuthenticationViewModel, this)
        viewModel.authenticationViewModel = biometricAuthenticationViewModel

        setupUI()
        observeUI()
    }

    private fun setupUI() {
        ui.optionsList.layoutManager = LinearLayoutManager(requireContext())
        ui.optionsList.adapter = optionsAdapter
    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(shareBugReport) { this@AllSettingsFragment.shareBugReport() }

        observe(openYatOnboarding) { yatAdapter.openOnboarding(requireActivity()) }

        observe(allSettingsOptions) { optionsAdapter.update(it) }
    }

    private fun shareBugReport() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                viewModel.bugReportingService.shareBugReport(requireActivity())
            } catch (e: BugReportingService.BugReportFileSizeLimitExceededException) {
                showBugReportFileSizeExceededDialog()
            }
        }
    }

    private fun showBugReportFileSizeExceededDialog() {
        val dialogBuilder = AlertDialog.Builder(context ?: return)
        val dialog = dialogBuilder.setMessage(string(debug_log_file_size_limit_exceeded_dialog_content))
            .setCancelable(false)
            .setPositiveButton(string(common_ok)) { dialog, _ -> dialog.cancel() }
            .setTitle(getString(debug_log_file_size_limit_exceeded_dialog_title))
            .create()
        dialog.show()
    }

    private fun processNavigation(navigation: AllSettingsNavigation) {
        when (navigation) {
            AllSettingsNavigation.ToBackgroundService -> toBackgroundService()
            AllSettingsNavigation.ToBackupSettings -> toBackupSettings()
            AllSettingsNavigation.ToBaseNodeSelection -> navigate(R.id.action_settingsFragment_to_changeBaseNodeFragment)
            AllSettingsNavigation.ToDeleteWallet -> toDeleteWallet()
            AllSettingsNavigation.ToNetworkSelection -> navigate(R.id.action_settingsFragment_to_networkSelectionFragment)
        }
    }

    private fun toBackupSettings() = startActivity(Intent(requireContext(), BackupSettingsActivity::class.java))

    private fun toDeleteWallet() = startActivity(Intent(requireContext(), DeleteWalletActivity::class.java))

    private fun toBackgroundService() = startActivity(Intent(requireContext(), BackgroundServiceSettingsActivity::class.java))

    override fun onDestroyView() {
        ui.scrollView.setOnScrollChangeListener(null)
        super.onDestroyView()
    }
}

