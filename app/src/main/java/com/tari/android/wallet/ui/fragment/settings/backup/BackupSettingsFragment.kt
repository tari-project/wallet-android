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
package com.tari.android.wallet.ui.fragment.settings.backup

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.core.animation.addListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.R.color.back_up_settings_permission_processing
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentWalletBackupSettingsBinding
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.infrastructure.backup.BackupDisabled
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.util.UiUtil.setColor
import com.tari.android.wallet.util.SharedPrefsWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.format.DateTimeFormat
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject

internal class BackupSettingsFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var backupStorage: BackupStorage

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var sharedPrefs: SharedPrefsWrapper

    private lateinit var ui: FragmentWalletBackupSettingsBinding
    private var optionsAnimation: Animator? = null

    private var backupOptionsAreVisible = true
    private var showDialogOnBackupFailedState = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentWalletBackupSettingsBinding.inflate(inflater, container, false)
        .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupCTAs()
        subscribeToBackupState()
        lifecycleScope.launch(Dispatchers.IO) {
            backupManager.checkStorageStatus()
        }

    }

    override fun onDestroyView() {
        EventBus.unsubscribeFromBackupState(this)
        super.onDestroyView()
    }

    private fun setupViews() {
        ui.backupPermissionProgressBar.setColor(color(back_up_settings_permission_processing))
        ui.cloudBackupStatusProgressView
            .setColor(color(R.color.all_settings_back_up_status_processing))
        ui.backupPermissionSwitch.isChecked = sharedPrefs.backupIsEnabled
        backupOptionsAreVisible = if (sharedPrefs.backupIsEnabled) {
            updatePasswordChangeLabel()
            showLastSuccessfulBackupDateTime()
            true
        } else {
            hideAllBackupOptions()
            ui.lastBackupTimeTextView.gone()
            false
        }
    }

    private fun updatePasswordChangeLabel() {
        ui.updatePasswordLabelTextView.text =
            if (sharedPrefs.backupPassword == null) string(back_up_wallet_set_backup_password_cta)
            else string(back_up_wallet_change_backup_password_cta)
    }

    private fun showLastSuccessfulBackupDateTime() {
        ui.lastBackupTimeTextView.visible()
        val date = sharedPrefs.lastSuccessfulBackupDate?.toLocalDateTime()
        if (date == null) {
            ui.lastBackupTimeTextView.text = ""
        } else {
            ui.lastBackupTimeTextView.text = string(
                back_up_wallet_last_successful_backup,
                BACKUP_DATE_FORMATTER.print(date),
                BACKUP_TIME_FORMATTER.print(date)
            )
        }
    }

    private fun subscribeToBackupState() {
        EventBus.subscribeToBackupState(this) { backupState ->
            lifecycleScope.launch(Dispatchers.Main) {
                onBackupStateChanged(backupState)
            }
        }
    }

    private fun onBackupStateChanged(backupState: BackupState) {
        when (backupState) {
            is BackupCheckingStorage -> {
                hideSwitchAndShowProgressBar(switchIsChecked = true)
                updateBackupNowButtonState(backupState)
            }
            is BackupDisabled -> {
                hideAllBackupOptionsWithAnimation()
                showSwitchAndHideProgressBar(switchIsChecked = false)
            }
            is BackupUpToDate -> {
                showSwitchAndHideProgressBar(switchIsChecked = true)
                showBackupOptionsWithAnimation()
                showLastSuccessfulBackupDateTime()
                updatePasswordChangeLabel()
                updateBackupNowButtonState(backupState)
            }
            is BackupFailed -> {
                if (!backupOptionsAreVisible) {
                    showBackupStorageSetupFailedDialog()
                    showSwitchAndHideProgressBar(switchIsChecked = false)
                } else {
                    showSwitchAndHideProgressBar(switchIsChecked = true)
                    if (showDialogOnBackupFailedState) {
                        showBackupFailureDialog(backupState.exception)
                    }
                }
                updateBackupNowButtonState(backupState)
            }
            is BackupScheduled -> {
                showSwitchAndHideProgressBar(switchIsChecked = true)
                updateBackupNowButtonState(backupState)
            }
            is BackupInProgress -> {
                hideSwitchAndShowProgressBar(switchIsChecked = true)
                updateBackupNowButtonState(backupState)
            }
        }
    }

    private fun setupCTAs() {
        ui.backCtaView.setOnClickListener(
            ThrottleClick { requireActivity().onBackPressed() }
        )
        ui.backupWithRecoveryPhraseCtaView.setOnClickListener(
            ThrottleClick {
                requireAuthorization {
                    (requireActivity() as SettingsRouter).toWalletBackupWithRecoveryPhrase(this)
                }
            }
        )
        ui.backupWalletToCloudCtaView.setOnClickListener(
            ThrottleClick {
                lifecycleScope.launch(Dispatchers.IO) {
                    backupManager.backup(isInitialBackup = false)
                }
            }
        )
        ui.updatePasswordCtaView.setOnClickListener(
            ThrottleClick {
                requireAuthorization {
                    val router = requireActivity() as SettingsRouter
                    if (sharedPrefs.backupPassword == null) {
                        router.toChangePassword(this)
                    } else {
                        router.toConfirmPassword(this)
                    }
                }
            }
        )
        setPermissionSwitchListener()
    }

    private fun setPermissionSwitchListener() {
        ui.backupPermissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            hideSwitchAndShowProgressBar(isChecked)
            if (isChecked) {
                setupBackupStorage()
            } else {
                clearBackup()
            }
        }
    }

    private fun hideSwitchAndShowProgressBar(switchIsChecked: Boolean) {
        ui.backupPermissionSwitch.invisible()
        ui.backupPermissionSwitch.isEnabled = false
        setSwitchCheck(switchIsChecked)
        ui.backupPermissionProgressBar.visible()
    }

    private fun setupBackupStorage() {
        backupStorage.setup(this)
    }

    private fun clearBackup() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                backupManager.clear()
            } catch (e: Exception) {
                Logger.e(e, "Error occurred during signing out")
                withContext(Dispatchers.Main) {
                    showTurnOffFailedDialog()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                backupStorage.onSetupActivityResult(
                    requestCode,
                    resultCode,
                    data
                )
                showDialogOnBackupFailedState = true
                backupManager.backup(isInitialBackup = true)
            } catch (exception: Exception) {
                Logger.e("Backup storage setup failed: $exception")
                withContext(Dispatchers.Main) {
                    showBackupStorageSetupFailedDialog()
                    showSwitchAndHideProgressBar(switchIsChecked = false)
                }
                EventBus.postBackupState(BackupFailed(exception))
            }
        }
    }

    private fun updateBackupNowButtonState(state: BackupState) {
        resetStatusIcons()
        when (state) {
            is BackupCheckingStorage -> {
                ui.backupWalletToCloudCtaView.isEnabled = false
                ui.backupNowTextView.alpha = ALPHA_DISABLED
                activateBackupStatusView(
                    ui.cloudBackupStatusProgressView,
                    back_up_wallet_backup_status_checking_backup,
                    R.color.all_settings_back_up_status_processing
                )
            }
            is BackupInProgress -> {
                ui.backupWalletToCloudCtaView.isEnabled = false
                ui.backupNowTextView.alpha = ALPHA_DISABLED
                activateBackupStatusView(
                    ui.cloudBackupStatusProgressView,
                    back_up_wallet_backup_status_in_progress,
                    R.color.all_settings_back_up_status_processing
                )
            }
            is BackupUpToDate -> {
                ui.backupWalletToCloudCtaView.isEnabled = false
                ui.backupNowTextView.alpha = ALPHA_DISABLED
                activateBackupStatusView(
                    ui.cloudBackupStatusSuccessView,
                    back_up_wallet_backup_status_up_to_date,
                    R.color.all_settings_back_up_status_up_to_date
                )
            }
            is BackupScheduled -> {
                ui.backupWalletToCloudCtaView.isEnabled = true
                ui.backupNowTextView.alpha = ALPHA_VISIBLE
                if (sharedPrefs.backupFailureDate == null) {
                    activateBackupStatusView(
                        ui.cloudBackupStatusSuccessView,
                        back_up_wallet_backup_status_scheduled,
                        R.color.all_settings_back_up_status_processing
                    )
                } else {
                    activateBackupStatusView(
                        ui.cloudBackupStatusWarningView,
                        back_up_wallet_backup_status_scheduled,
                        R.color.all_settings_back_up_status_processing
                    )
                }
            }
            is BackupFailed -> {
                ui.backupWalletToCloudCtaView.isEnabled = true
                ui.backupNowTextView.alpha = ALPHA_VISIBLE
                activateBackupStatusView(
                    ui.cloudBackupStatusWarningView,
                    back_up_wallet_backup_status_outdated,
                    R.color.all_settings_back_up_status_error
                )
            }
            else -> { /* no-op */
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
        val hideText = textId == -1
        ui.backupStatusTextView.text = if (hideText) "" else string(textId)
        ui.backupStatusTextView.visibility = if (hideText) View.GONE else View.VISIBLE
        if (textColor != -1) ui.backupStatusTextView.setTextColor(color(textColor))
    }

    private fun showSwitchAndHideProgressBar(switchIsChecked: Boolean) {
        ui.backupPermissionProgressBar.gone()
        ui.backupPermissionSwitch.isEnabled = true
        setSwitchCheck(switchIsChecked)
        ui.backupPermissionSwitch.visible()
    }

    private fun setSwitchCheck(isChecked: Boolean) {
        ui.backupPermissionSwitch.setOnCheckedChangeListener(null)
        ui.backupPermissionSwitch.isChecked = isChecked
        setPermissionSwitchListener()
    }

    private fun showBackupOptionsWithAnimation() {
        if (backupOptionsAreVisible) {
            return
        }
        val views = arrayOf(
            ui.backupsSeparatorView,
            ui.updatePasswordCtaView,
            ui.lastBackupTimeTextView,
            ui.backupWalletToCloudCtaContainerView
        )
        optionsAnimation?.cancel()
        optionsAnimation = ValueAnimator.ofFloat(ALPHA_INVISIBLE, ALPHA_VISIBLE).apply {
            duration = OPTIONS_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                views.forEach { v -> v.alpha = alpha }
            }
            addListener(
                onStart = {
                    views.forEach { v ->
                        v.visible()
                        v.alpha = ALPHA_INVISIBLE
                    }
                },
                onCancel = { views.forEach { v -> v.alpha = ALPHA_VISIBLE } }
            )
            start()
        }
        backupOptionsAreVisible = true
    }

    private fun hideAllBackupOptionsWithAnimation() {
        if (!backupOptionsAreVisible) {
            return
        }
        val views = arrayOf(
            ui.backupsSeparatorView,
            ui.updatePasswordCtaView,
            ui.backupWalletToCloudCtaContainerView,
            ui.lastBackupTimeTextView
        )
        val wasClickable = views.map { it.isClickable }
        optionsAnimation?.cancel()
        optionsAnimation = ValueAnimator.ofFloat(ALPHA_VISIBLE, ALPHA_INVISIBLE).apply {
            duration = OPTIONS_ANIMATION_DURATION
            interpolator = LinearInterpolator()
            addUpdateListener {
                val alpha = it.animatedValue as Float
                views.forEach { v -> v.alpha = alpha }
            }
            val finalizeAnimation: (Animator?) -> Unit = {
                views.zip(wasClickable).forEach { (view, wasClickable) ->
                    view.isClickable = wasClickable
                    view.gone()
                    view.alpha = ALPHA_VISIBLE
                }
            }
            addListener(
                onStart = {
                    views.forEach { v ->
                        v.isClickable = false
                        v.alpha = ALPHA_VISIBLE
                    }
                },
                onEnd = finalizeAnimation,
                onCancel = finalizeAnimation
            )
            start()
        }
        backupOptionsAreVisible = false
    }

    private fun hideAllBackupOptions() {
        if (!backupOptionsAreVisible) {
            return
        }
        arrayOf(
            ui.backupsSeparatorView,
            ui.updatePasswordCtaView,
            ui.backupWalletToCloudCtaContainerView,
            ui.lastBackupTimeTextView
        ).forEach(View::gone)
        backupOptionsAreVisible = false
    }

    private fun showBackupStorageSetupFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_storage_setup_error_title),
            description = string(back_up_wallet_storage_setup_error_desc),
            onClose = ::resetSwitchState
        ).show()
    }

    private fun showTurnOffFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_sign_out_error_title),
            description = string(back_up_wallet_sign_out_error_desc),
            onClose = ::resetSwitchState
        ).show()
    }

    private fun resetSwitchState() {
        ui.backupPermissionSwitch.isEnabled = true
        ui.backupPermissionProgressBar.gone()
        ui.backupPermissionSwitch.visible()
    }

    private fun showBackupFailureDialog(e: Exception?) {
        val errorMessage = when {
            e is UnknownHostException -> string(error_no_connection_title)
            e?.message == null -> string(back_up_wallet_backing_up_unknown_error)
            else -> string(back_up_wallet_backing_up_error_desc, e.message!!)
        }
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_backing_up_error_title),
            description = errorMessage
        ).show()
    }

    private fun resetStatusIcons() {
        ui.backupWalletToCloudCtaView.isEnabled = true
        ui.cloudBackupStatusProgressView.invisible()
        ui.cloudBackupStatusSuccessView.invisible()
        ui.cloudBackupStatusWarningView.invisible()
        ui.backupWithRecoveryPhraseWarningView.gone()
    }

    private fun requireAuthorization(onAuthorized: () -> Unit) {
        if (authService.isDeviceSecured) {
            lifecycleScope.launch {
                try {
                    // prompt system authentication dialog
                    authService.authenticate(
                        this@BackupSettingsFragment,
                        title = string(auth_title),
                        subtitle =
                        if (authService.isBiometricAuthAvailable) string(auth_biometric_prompt)
                        else string(auth_device_lock_code_prompt)
                    )
                    onAuthorized()
                } catch (e: BiometricAuthenticationService.BiometricAuthenticationException) {
                    if (e.code != ERROR_USER_CANCELED && e.code != ERROR_CANCELED)
                        Logger.e("Other biometric error. Code: ${e.code}")
                    showAuthenticationCancellationError()
                }
            }
        } else {
            onAuthorized()
        }
    }

    private fun showAuthenticationCancellationError() {
        AlertDialog.Builder(requireContext())
            .setCancelable(false)
            .setMessage(getString(auth_failed_desc))
            .setNegativeButton(string(exit)) { dialog, _ -> dialog.cancel() }
            .create()
            .apply { setTitle(string(auth_failed_title)) }
            .show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = BackupSettingsFragment()
            .apply { arguments = Bundle() }

        private val BACKUP_DATE_FORMATTER =
            DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
        private const val OPTIONS_ANIMATION_DURATION = 500L
        private const val ALPHA_INVISIBLE = 0F
        private const val ALPHA_VISIBLE = 1F
        private const val ALPHA_DISABLED = 0.15F
    }

}
