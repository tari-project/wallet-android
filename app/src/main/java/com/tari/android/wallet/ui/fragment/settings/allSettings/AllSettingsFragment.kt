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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R.color.all_settings_back_up_status_processing
import com.tari.android.wallet.R.dimen.menu_item_height
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentAllSettingsBinding
import com.tari.android.wallet.di.DiContainer.appComponent
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.yat.YatAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

internal class AllSettingsFragment : CommonFragment<FragmentAllSettingsBinding, AllSettingsViewModel>() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var bugReportingService: BugReportingService

    @Inject
    lateinit var clipboardManager: ClipboardManager

    @Inject
    lateinit var yatAdapter: YatAdapter

    private val biometricAuthenticationViewModel: BiometricAuthenticationViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

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
        ui.cloudBackupStatusProgressView.setColor(color(all_settings_back_up_status_processing))
        ui.scrollElevationGradientView.alpha = 0f
        ui.scrollView.setOnScrollChangeListener(ScrollListener())
        bindCTAs()
    }

    private fun bindCTAs() = with(ui) {
        reportBugCtaView.setOnThrottledClickListener { viewModel.shareBugReport() }
        visitSiteCtaView.setOnThrottledClickListener { viewModel.openTariUrl() }
        contributeCtaView.setOnThrottledClickListener { viewModel.openGithubUrl() }
        userAgreementCtaView.setOnThrottledClickListener { viewModel.openAgreementUrl() }
        privacyPolicyCtaView.setOnThrottledClickListener { viewModel.openPrivateUrl() }
        disclaimerCtaView.setOnThrottledClickListener { viewModel.openDisclaimerUrl() }
        explorerCtaView.setOnThrottledClickListener { viewModel.openExplorerUrl() }
        backUpWalletCtaView.setOnThrottledClickListener { viewModel.navigateToBackupSettings() }
        backgroundServiceCtaView.setOnThrottledClickListener { viewModel.navigateToBackgroundServiceSettings() }
        torBridgesCta.setOnThrottledClickListener { viewModel.navigateToTorBridgesSettings() }
        changeBaseNodeCtaView.setOnThrottledClickListener { viewModel.navigateToBaseNodeSelection() }
        changeNetworkCtaView.setOnThrottledClickListener { viewModel.navigateToNetworkSelection() }
        backgroundServiceCtaView.setOnThrottledClickListener { viewModel.navigateToBackgroundServiceSettings() }
        deleteWalletCtaView.setOnThrottledClickListener { viewModel.navigateToDeleteWallet() }
        connectYats.setOnThrottledClickListener { yatAdapter.openOnboarding(requireActivity()) }
        networkInfoCtaView.setOnThrottledClickListener { copy(viewModel.versionInfo.value.orEmpty()) }
    }

    private fun observeUI() = with(viewModel) {
        observe(navigation) { processNavigation(it) }

        observe(shareBugReport) { this@AllSettingsFragment.shareBugReport() }

        observe(backupState) { activateBackupStatusView(it) }

        observe(lastBackupDate) { ui.lastBackupTimeTextView.text = it }

        observe(versionInfo) { ui.networkInfoTextView.text = it }
    }

    private fun activateBackupStatusView(backupState: PresentationBackupState) {
        val iconView = when (backupState.status) {
            PresentationBackupState.BackupStateStatus.InProgress -> ui.cloudBackupStatusProgressView
            PresentationBackupState.BackupStateStatus.Success -> ui.cloudBackupStatusSuccessView
            PresentationBackupState.BackupStateStatus.Warning -> ui.cloudBackupStatusWarningView
            PresentationBackupState.BackupStateStatus.Scheduled -> ui.cloudBackupStatusScheduledView
        }

        fun View.adjustVisibility() {
            visibility = if (this == iconView) View.VISIBLE else View.INVISIBLE
        }

        ui.cloudBackupStatusProgressView.adjustVisibility()
        ui.cloudBackupStatusSuccessView.adjustVisibility()
        ui.cloudBackupStatusWarningView.adjustVisibility()
        ui.cloudBackupStatusScheduledView.adjustVisibility()
        val hideText = backupState.textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(backupState.textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (backupState.textColor != -1) ui.backupStatusTextView.setTextColor(color(backupState.textColor))
    }

    private fun shareBugReport() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bugReportingService.shareBugReport(requireActivity())
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
        val router = requireActivity() as AllSettingsRouter

        when (navigation) {
            AllSettingsNavigation.ToBackgroundService -> router.toBackgroundService()
            AllSettingsNavigation.ToBackupSettings -> router.toBackupSettings()
            AllSettingsNavigation.ToBaseNodeSelection -> router.toBaseNodeSelection()
            AllSettingsNavigation.ToDeleteWallet -> router.toDeleteWallet()
            AllSettingsNavigation.ToNetworkSelection -> router.toNetworkSelection()
            AllSettingsNavigation.ToTorBridges -> router.toTorBridges()
        }
    }

    private fun copy(text: String) {
        clipboardManager.setPrimaryClip(ClipData.newPlainText(string(all_settings_version_text_copy_title), text))
        Toast.makeText(requireActivity(), string(all_settings_version_text_copy_toast_message), Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        ui.scrollView.setOnScrollChangeListener(null)
        super.onDestroyView()
    }

    override fun onDestroy() {
        EventBus.backupState.unsubscribe(this)
        super.onDestroy()
    }

    inner class ScrollListener : View.OnScrollChangeListener {

        override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
            ui.scrollElevationGradientView.alpha =
                min(Constants.UI.scrollDepthShadowViewMaxOpacity, scrollY / (dimenPx(menu_item_height)).toFloat())
        }
    }

    companion object {
        fun newInstance() = AllSettingsFragment()
    }
}

