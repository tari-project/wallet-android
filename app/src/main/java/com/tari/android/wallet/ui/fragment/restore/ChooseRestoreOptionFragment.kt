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
package com.tari.android.wallet.ui.fragment.restore

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.all_settings_back_up_status_processing
import com.tari.android.wallet.R.string.back_up_wallet_storage_setup_error_desc
import com.tari.android.wallet.R.string.back_up_wallet_storage_setup_error_title
import com.tari.android.wallet.R.string.restore_wallet_error_title
import com.tari.android.wallet.R.string.restore_wallet_error_desc
import com.tari.android.wallet.R.string.error_no_connection_title
import com.tari.android.wallet.R.string.restore_wallet_error_file_not_found
import com.tari.android.wallet.databinding.FragmentChooseRestoreOptionBinding
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.util.UiUtil.setColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class ChooseRestoreOptionFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var backupStorage: BackupStorage

    private lateinit var ui: FragmentChooseRestoreOptionBinding
    private val blockingBackPressDispatcher = object : OnBackPressedCallback(false) {
        // No-op by design
        override fun handleOnBackPressed() = Unit
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        appComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        FragmentChooseRestoreOptionBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher
            .addCallback(viewLifecycleOwner, blockingBackPressDispatcher)
        ui.backCtaView.setOnClickListener { requireActivity().onBackPressed() }
        ui.restoreWalletMenuItemProgressView.setColor(color(all_settings_back_up_status_processing))
        ui.restoreWalletMenuItemProgressView.gone()
        ui.restoreWalletCtaView.setOnClickListener {
            beginProgress()
            backupStorage.setup(this)
        }
        ui.restoreWithRecoveryPhraseCtaView.setOnClickListener {
            (requireActivity() as WalletRestoreRouter).toRestoreWithRecoveryPhrase()
        }
    }

    private fun beginProgress() {
        blockingBackPressDispatcher.isEnabled = true
        ui.restoreWalletMenuItemProgressView.visible()
        ui.restoreWalletMenuItemArrowImageView.invisible()
        ui.restoreWalletCtaView.isEnabled = false
    }

    private fun endProgress() {
        blockingBackPressDispatcher.isEnabled = false
        ui.restoreWalletMenuItemProgressView.invisible()
        ui.restoreWalletMenuItemArrowImageView.visible()
        ui.restoreWalletCtaView.isEnabled = true
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
            } catch (exception: Exception) {
                Logger.e("Backup storage setup failed: $exception")
                backupStorage.signOut()
                withContext(Dispatchers.Main) {
                    endProgress()
                    showAuthFailedDialog()
                }
                return@launch
            }
            restore()
        }
    }

    private suspend fun restore() {
        try {
            // try to restore with no password
            backupStorage.restoreLatestBackup()
            (requireActivity() as WalletRestoreRouter).toRestoreInProgress()
        } catch (exception: Exception){
            when(exception) {
                is BackupStorageAuthRevokedException -> {
                    Logger.e(exception, "Auth revoked.")
                    backupStorage.signOut()
                    withContext(Dispatchers.Main) {
                        endProgress()
                        showAuthFailedDialog()
                    }
                }
                is BackupStorageTamperedException -> { // backup file not found
                    Logger.e(exception, "Backup file not found.")
                    backupStorage.signOut()
                    withContext(Dispatchers.Main) {
                        endProgress()
                        showBackupFileNotFoundDialog()
                    }
                }
                is BackupFileIsEncryptedException -> {
                    withContext(Dispatchers.Main) { endProgress() }
                    (requireActivity() as WalletRestoreRouter).toEnterRestorePassword()
                }
                is IOException -> {
                    Logger.e(exception, "Restore failed: network connection.")
                    backupStorage.signOut()
                    withContext(Dispatchers.Main) {
                        endProgress()
                        showRestoreFailedDialog(string(error_no_connection_title))
                    }
                }
                else -> {
                    Logger.e(exception, "Restore failed: $exception")
                    backupStorage.signOut()
                    withContext(Dispatchers.Main) {
                        endProgress()
                        showRestoreFailedDialog(exception.message)
                    }
                }
            }
        }
    }

    private fun showAuthFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_storage_setup_error_title),
            description = string(back_up_wallet_storage_setup_error_desc)
        ).show()
    }

    private fun showBackupFileNotFoundDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_storage_setup_error_title),
            description = string(restore_wallet_error_file_not_found),
            onClose = {
                requireActivity().onBackPressed()
            }
        ).show()
    }

    private fun showRestoreFailedDialog(message: String? = null) {
        ErrorDialog(
            requireContext(),
            title = string(restore_wallet_error_title),
            description = message ?: string(restore_wallet_error_desc)
        ).show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = ChooseRestoreOptionFragment()
    }

}
