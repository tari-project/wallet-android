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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt.ERROR_CANCELED
import androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.databinding.FragmentWalletBackupSettingsBinding
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.activity.settings.SettingsRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import kotlinx.coroutines.launch
import javax.inject.Inject

class WalletBackupSettingsFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var service: BiometricAuthenticationService

    private lateinit var ui: FragmentWalletBackupSettingsBinding
    private lateinit var vm: StorageBackupViewModel
    private lateinit var blockingBackPressedDispatcher: OnBackPressedCallback

    override fun onAttach(context: Context) {
        super.onAttach(context)
        backupAndRestoreComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = FragmentWalletBackupSettingsBinding.inflate(inflater, container, false)
        .also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        vm = ViewModelProvider(requireActivity()).get(StorageBackupViewModel::class.java)
        vm.state.observe(viewLifecycleOwner, Observer(::handleBackupState))
        ui.backCtaView.setOnClickListener(ThrottleClick { requireActivity().onBackPressed() })
        ui.backUpWithRecoveryPhraseCtaView.setOnClickListener(ThrottleClick {
            requireAuthorization {
                (requireActivity() as SettingsRouter).toWalletBackupWithRecoveryPhrase()
            }
        })
        ui.backUpWalletToCloudCtaView.setOnClickListener(ThrottleClick {
            requireAuthorization { performBackup() }
        })
        blockingBackPressedDispatcher = object : OnBackPressedCallback(false) {
            // No-op by design
            override fun handleOnBackPressed() = Unit
        }
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressedDispatcher)
    }

    private fun handleBackupState(state: StorageBackupState) {
        resetStatusIcons()
        handleBackupCheckState(state)
        handleBackupProcessState(state)
    }

    private fun handleBackupCheckState(state: StorageBackupState) = when (state.backupStatus) {
        StorageBackupStatus.CHECKING_STATUS -> {
            ui.backUpWalletToCloudCtaView.isEnabled = false
            ui.cloudBackUpStatusProgressView.visible()
        }
        StorageBackupStatus.STATUS_CHECK_FAILURE -> {
            handleStatusCheckFailure(state)
        }
        StorageBackupStatus.BACKED_UP -> ui.cloudBackUpStatusSuccessView.visible()
        StorageBackupStatus.NOT_BACKED_UP, StorageBackupStatus.UNKNOWN -> {
            ui.cloudBackUpStatusWarningView.visible()
            ui.backUpWithRecoveryPhraseWarningView.visible()
        }
    }

    private fun handleStatusCheckFailure(state: StorageBackupState) {
        val exception = state.statusCheckException!!
        if (exception is UserRecoverableAuthIOException) {
            startActivityForResult(exception.intent, REQUEST_CODE_REAUTH)
        } else {
            displayStatusCheckFailureDialog(
                exception.message ?: string(back_up_wallet_status_check_unknown_error)
            )
            vm.clearStatusCheckFailure()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_REAUTH) {
            if (resultCode == Activity.RESULT_OK) {
                vm.clearStatusCheckFailure()
                vm.checkBackupStatus()
            } else {
                displayStatusCheckFailureDialog(
                    string(back_up_wallet_status_check_authentication_cancellation)
                )
                vm.clearStatusCheckFailure()
            }
        }
    }

    private fun displayStatusCheckFailureDialog(message: String) {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_back_up_check_error_title),
            description = string(
                back_up_wallet_back_up_check_error_desc,
                message
            )
        ).show()
    }

    private fun handleBackupProcessState(state: StorageBackupState) {
        blockingBackPressedDispatcher.isEnabled =
            state.processStatus == BackupProcessStatus.BACKING_UP
        when (state.processStatus) {
            BackupProcessStatus.BACKING_UP -> {
                resetStatusIcons()
                ui.backUpWalletToCloudCtaView.isEnabled = false
                ui.cloudBackUpStatusProgressView.visible()
            }
            BackupProcessStatus.FAILURE -> {
                displayBackingUpFailureDialog(state.processException)
                vm.resetProcessStatus()
            }
            BackupProcessStatus.SUCCESS -> vm.resetProcessStatus()
            BackupProcessStatus.IDLE -> {
                // No-op
            }
        }
    }

    private fun displayBackingUpFailureDialog(e: Exception?) {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_backing_up_error_title),
            description = string(
                back_up_wallet_backing_up_error_desc,
                e?.message ?: ""
            )
        ).show()
    }

    private fun resetStatusIcons() {
        ui.backUpWalletToCloudCtaView.isEnabled = true
        ui.cloudBackUpStatusProgressView.invisible()
        ui.cloudBackUpStatusSuccessView.invisible()
        ui.cloudBackUpStatusWarningView.invisible()
        ui.backUpWithRecoveryPhraseWarningView.gone()
    }

    private fun performBackup() {
        try {
            vm.backup(charArrayOf())
        } catch (e: IllegalStateException) {
            ErrorDialog(
                requireContext(),
                title = string(back_up_wallet_back_up_is_in_progress_error),
                description = e.message ?: ""
            ).show()
        }
    }

    private fun requireAuthorization(onAuthorized: () -> Unit) {
        if (service.isDeviceSecured) {
            lifecycleScope.launch {
                try {
                    // prompt system authentication dialog
                    service.authenticate(
                        this@WalletBackupSettingsFragment,
                        title = string(auth_title),
                        subtitle =
                        if (service.isBiometricAuthAvailable) string(auth_biometric_prompt)
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
        fun newInstance() = WalletBackupSettingsFragment()

        private const val REQUEST_CODE_REAUTH = 1355
    }

}
