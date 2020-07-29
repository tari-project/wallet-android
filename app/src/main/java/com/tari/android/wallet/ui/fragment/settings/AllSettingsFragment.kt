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
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentAllSettingsBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.BugReportingService
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil.setColor
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.format.DateTimeFormat
import java.io.IOException
import java.util.*
import javax.inject.Inject

internal class AllSettingsFragment @Deprecated(
    """Use newInstance() and supply all the necessary 
data via arguments instead, as fragment's default no-op constructor is used by the framework for 
UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var sharedPrefs: SharedPrefsWrapper

    @Inject
    lateinit var backupManager: BackupManager

    private lateinit var ui: FragmentAllSettingsBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentAllSettingsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        EventBus.subscribeToBackupState(this) { backupState ->
            lifecycleScope.launch(Dispatchers.Main) {
                onBackupStateChanged(backupState)
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            checkStorageStatus()
        }
    }

    private fun setupUI() {
        ui.cloudBackupStatusProgressView.setColor(color(all_settings_back_up_status_processing))
        bindCTAs()
    }

    private fun bindCTAs() {
        ui.reportBugCtaView.setOnClickListener { shareBugReport() }
        ui.visitSiteCtaView.setOnClickListener { openLink(string(tari_url)) }
        ui.contributeCtaView.setOnClickListener { openLink(string(github_repo_url)) }
        ui.userAgreementCtaView.setOnClickListener { openLink(string(user_agreement_url)) }
        ui.privacyPolicyCtaView.setOnClickListener { openLink(string(privacy_policy_url)) }
        ui.disclaimerCtaView.setOnClickListener { openLink(string(disclaimer_url)) }
        ui.backUpWalletCtaView.setOnClickListener { navigateToBackupSettings() }
    }

    override fun onDestroy() {
        EventBus.unsubscribeFromBackupState(this)
        super.onDestroy()
    }

    private suspend fun checkStorageStatus() {
        try {
            backupManager.checkStorageStatus()
        } catch (e: BackupStorageAuthRevokedException) {
            Logger.e("Backup storage auth error.")
            // show access revoked information
            withContext(Dispatchers.Main) {
                showBackupStorageCheckFailedDialog(
                    string(check_backup_storage_status_auth_revoked_error_description)
                )
            }
        } catch (e: IOException) {
            Logger.e("Backup storage I/O (access) error.")
            withContext(Dispatchers.Main) {
                showBackupStorageCheckFailedDialog(
                    string(check_backup_storage_status_access_error_description)
                )
            }
        } catch (e: Exception) {
            Logger.e("Backup storage tampered.")
            withContext(Dispatchers.Main) {
                updateLastSuccessfulBackupDate()
            }
        }
    }

    private fun updateLastSuccessfulBackupDate() {
        ui.lastBackupTimeTextView.visible()
        val time = sharedPrefs.lastSuccessfulBackupDate?.toLocalDateTime()
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

    private fun onBackupStateChanged(backupState: BackupState?) {
        if (backupState == null) {
            ui.cloudBackupStatusProgressView.invisible()
            ui.cloudBackupStatusSuccessView.invisible()
            ui.cloudBackupStatusWarningView.visible()
            ui.backupStatusTextView.text = ""
            ui.lastBackupTimeTextView.gone()
        } else {
            when (backupState) {
                is BackupDisabled -> {
                    updateLastSuccessfulBackupDate()
                    activateBackupStatusView(ui.cloudBackupStatusWarningView)
                }
                is BackupCheckingStorage -> {
                    updateLastSuccessfulBackupDate()
                    activateBackupStatusView(
                        ui.cloudBackupStatusProgressView,
                        back_up_wallet_backup_status_checking_backup,
                        all_settings_back_up_status_processing
                    )
                }
                is BackupStorageCheckFailed -> {
                    updateLastSuccessfulBackupDate()
                    activateBackupStatusView(
                        icon = ui.cloudBackupStatusWarningView,
                        textColor = all_settings_back_up_status_error
                    )
                }
                is BackupScheduled -> {
                    updateLastSuccessfulBackupDate()
                    if (sharedPrefs.backupFailureDate == null) {
                        activateBackupStatusView(
                            ui.cloudBackupStatusScheduledView,
                            back_up_wallet_backup_status_scheduled,
                            all_settings_back_up_status_scheduled
                        )
                    } else {
                        activateBackupStatusView(
                            ui.cloudBackupStatusWarningView,
                            back_up_wallet_backup_status_scheduled,
                            all_settings_back_up_status_processing
                        )
                    }
                }
                is BackupInProgress -> {
                    updateLastSuccessfulBackupDate()
                    activateBackupStatusView(
                        ui.cloudBackupStatusProgressView,
                        back_up_wallet_backup_status_in_progress,
                        all_settings_back_up_status_processing
                    )
                }
                is BackupUpToDate -> {
                    updateLastSuccessfulBackupDate()
                    activateBackupStatusView(
                        ui.cloudBackupStatusSuccessView,
                        back_up_wallet_backup_status_up_to_date,
                        all_settings_back_up_status_up_to_date
                    )
                }
                is BackupOutOfDate -> {
                    updateLastSuccessfulBackupDate()
                    activateBackupStatusView(
                        ui.cloudBackupStatusWarningView,
                        back_up_wallet_backup_status_outdated,
                        all_settings_back_up_status_error
                    )
                }
            }
        }
    }

    private fun activateBackupStatusView(icon: View?, textId: Int = -1, textColor: Int = -1) {
        fun View.adjustVisibility() {
            visibility = if (this == icon) View.VISIBLE else View.INVISIBLE
        }
        ui.cloudBackupStatusProgressView.adjustVisibility()
        ui.cloudBackupStatusSuccessView.adjustVisibility()
        ui.cloudBackupStatusWarningView.adjustVisibility()
        ui.cloudBackupStatusScheduledView.adjustVisibility()
        val hideText = textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (textColor != -1) ui.backupStatusTextView.setTextColor(color(textColor))
    }

    private fun openLink(link: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
    }

    private fun navigateToBackupSettings() {
        (requireActivity() as AllSettingsRouter).toBackupSettings()
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

    private fun showBackupStorageCheckFailedDialog(message: String) {
        ErrorDialog(
            requireContext(),
            title = string(check_backup_storage_status_error_title),
            description = message
        ).show()
    }

    interface AllSettingsRouter {
        fun toBackupSettings()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = AllSettingsFragment()
        private val BACKUP_DATE_FORMATTER =
            DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
    }

}
