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
package com.tari.android.wallet.ui.fragment.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentAllSettingsBinding
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.infrastructure.backup.WalletBackup
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorage
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorageFactory
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.fragment.settings.backup.*
import com.tari.android.wallet.ui.util.UiUtil.setColor
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.format.DateTimeFormat
import javax.inject.Inject

class AllSettingsFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var factory: BackupStorageFactory

    @Inject
    lateinit var backup: WalletBackup

    @Inject
    lateinit var sharedPrefs: SharedPrefsWrapper

    private var backupViewModel: StorageBackupViewModel? = null

    private lateinit var ui: FragmentAllSettingsBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        backupAndRestoreComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentAllSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        ui.cloudBackupStatusProgressView.setColor(color(all_settings_back_up_status_processing))
        setupViewModel()
        bindToViewModelState()
        bindCTAs()
    }

    private fun showLastSuccessfulBackupTime() {
        ui.lastBackupTimeTextView.visible()
        val time = sharedPrefs.lastSuccessfulBackupDateTime?.toLocalDateTime()
        if (time == null) {
            ui.lastBackupTimeTextView.text = ""
        } else {
            ui.lastBackupTimeTextView.text = string(
                back_up_wallet_last_successful_backup,
                BACKUP_DATE_FORMATTER.print(time),
                BACKUP_TIME_FORMATTER.print(time)
            )
        }
    }

    private fun setupViewModel() {
        val lastSignInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (lastSignInAccount != null) {
            val storage = factory.google(requireContext(), lastSignInAccount)
            backupViewModel =
                ViewModelProvider(requireActivity(), viewModelFactory(storage))
                    .get(StorageBackupViewModel::class.java)
        }
    }

    private fun bindToViewModelState() {
        val vm = backupViewModel
        if (vm == null) {
            ui.cloudBackupStatusProgressView.invisible()
            ui.cloudBackupStatusSuccessView.invisible()
            ui.cloudBackupStatusWarningView.visible()
            ui.backupStatusTextView.text = ""
            ui.lastBackupTimeTextView.gone()
        } else {
            showLastSuccessfulBackupTime()
            vm.state.observe(viewLifecycleOwner, Observer(::refreshUiAccordingToState))
        }
    }

    private fun refreshUiAccordingToState(state: StorageBackupState) = when {
        state.processStatus == BackupProcessStatus.BACKING_UP -> activateBackupStatusView(
            ui.cloudBackupStatusProgressView,
            back_up_wallet_backup_status_in_progress,
            all_settings_back_up_status_processing
        )
        state.backupStatus == StorageBackupStatus.CHECKING_STATUS -> activateBackupStatusView(
            ui.cloudBackupStatusProgressView,
            back_up_wallet_backup_status_checking_backup,
            all_settings_back_up_status_processing
        )
        state.backupStatus == StorageBackupStatus.BACKED_UP -> activateBackupStatusView(
            ui.cloudBackupStatusSuccessView,
            back_up_wallet_backup_status_actual,
            all_settings_back_up_status_up_to_date
        )
        state.backupStatus == StorageBackupStatus.NOT_BACKED_UP -> activateBackupStatusView(
            ui.cloudBackupStatusWarningView,
            back_up_wallet_backup_status_outdated,
            all_settings_back_up_status_error
        )
        else -> activateBackupStatusView(ui.cloudBackupStatusWarningView)
    }

    private fun activateBackupStatusView(icon: View?, textId: Int = -1, textColor: Int = -1) {
        fun View.adjustVisibility() {
            visibility = if (this == icon) View.VISIBLE else View.INVISIBLE
        }
        ui.cloudBackupStatusProgressView.adjustVisibility()
        ui.cloudBackupStatusSuccessView.adjustVisibility()
        ui.cloudBackupStatusWarningView.adjustVisibility()
        val hideText = textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (textColor != -1) ui.backupStatusTextView.setTextColor(color(textColor))
    }

    private fun bindCTAs() {
        ui.doneCtaView.setOnClickListener { requireActivity().onBackPressed() }
        ui.reportBugCtaView.setOnClickListener { shareBugReport() }
        ui.visitSiteCtaView.setOnClickListener { openLink(string(tari_url)) }
        ui.contributeCtaView.setOnClickListener { openLink(string(github_repo_url)) }
        ui.userAgreementCtaView.setOnClickListener { openLink(string(user_agreement_url)) }
        ui.privacyPolicyCtaView.setOnClickListener { openLink(string(privacy_policy_url)) }
        ui.disclaimerCtaView.setOnClickListener { openLink(string(disclaimer_url)) }
        ui.backUpWalletCtaView.setOnClickListener {
            navigateToBackUpSettings()
        }
    }

    private fun openLink(link: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(requireContext())
            if (backupViewModel == null && lastSignedInAccount != null) {
                tryToInitializeViewModel()
            } else if (backupViewModel != null && lastSignedInAccount == null) {
                backupViewModel = null
                ui.lastBackupTimeTextView.gone()
                bindToViewModelState()
            }
        }
    }

    private fun tryToInitializeViewModel() {
        try {
            backupViewModel =
                ViewModelProvider(requireActivity()).get(StorageBackupViewModel::class.java)
            bindToViewModelState()
        } catch (ignored: Exception) {
        }
    }

    private fun viewModelFactory(storage: BackupStorage) =
        StorageBackupViewModelFactory(storage, backup, sharedPrefs)

    private fun navigateToBackUpSettings() {
        (requireActivity() as SettingsRouter).toWalletBackupSettings()
    }

    private fun shareBugReport() {
        val mContext = context ?: return
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                appComponent.bugReportingService.shareBugReport(mContext)
            } catch (e: BugReportingService.BugReportFileSizeLimitExceededException) {
                withContext(Dispatchers.Main) { showBugReportFileSizeExceededDialog() }
            }
        }
    }

    private fun showBugReportFileSizeExceededDialog() {
        val dialogBuilder = AlertDialog.Builder(context ?: return)
        val dialog = dialogBuilder.setMessage(
            string(debug_log_file_size_limit_exceeded_dialog_content)
        )
            .setCancelable(false)
            .setPositiveButton(string(common_ok)) { dialog, _ ->
                dialog.cancel()
            }
            .setTitle(getString(debug_log_file_size_limit_exceeded_dialog_title))
            .create()
        dialog.show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = AllSettingsFragment()
        private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy")
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
    }

}
