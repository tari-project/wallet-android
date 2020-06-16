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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.string.back_up_wallet_authentication_error_desc
import com.tari.android.wallet.R.string.back_up_wallet_authentication_error_title
import com.tari.android.wallet.databinding.FragmentChooseRestoreOptionBinding
import com.tari.android.wallet.infrastructure.backup.WalletRestorationFactory
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorageFactory
import com.tari.android.wallet.ui.activity.restore.WalletRestoreRouter
import com.tari.android.wallet.ui.dialog.ErrorDialog
import com.tari.android.wallet.ui.extension.appComponent
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.restore.RestorationWithCloudFragment.RestorationWithCloudState
import com.tari.android.wallet.ui.fragment.restore.RestorationWithCloudFragment.RestorationWithCloudStateFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class ChooseRestoreOptionFragment @Deprecated(
    """Use newInstance() and supply all the 
necessary data via arguments instead, as fragment's default no-op constructor is used by the 
framework for UI tree rebuild on configuration changes"""
) constructor() : Fragment() {

    @Inject
    lateinit var storageFactory: BackupStorageFactory

    @Inject
    lateinit var restorationFactory: WalletRestorationFactory

    private lateinit var ui: FragmentChooseRestoreOptionBinding

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
        ui.backCtaView.setOnClickListener { requireActivity().onBackPressed() }
        ui.backUpWalletWithCloudCtaView
            .setOnClickListener { processRestorationWithCloudIntent() }
        ui.backUpWithRecoveryPhraseCtaView
            .setOnClickListener {
                (requireActivity() as WalletRestoreRouter).toBackupWithRecoveryPhrase()
            }
    }

    private fun processRestorationWithCloudIntent() {
        try {
            ViewModelProvider(requireActivity()).get(RestorationWithCloudState::class.java)
                .restoreWallet()
            navigateToRestorationWithCloud()
        } catch (e: Exception) {
            // Means that we aren't authenticated yet
            requestAuthenticationForGDrive()
        }
    }

    private fun requestAuthenticationForGDrive() {
        val signInOptions =
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
                .build()
        val client = GoogleSignIn.getClient(requireActivity(), signInOptions)
        startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            if (resultCode == Activity.RESULT_OK) {
                setupBackupProcessAndNavigateToBackupSettings(data)
            } else {
                showAuthFailedDialog()
            }
        }
    }

    private fun setupBackupProcessAndNavigateToBackupSettings(data: Intent?) {
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val storage = storageFactory.google(requireActivity(), data)
                // Creating a shared viewmodel for further use
                ViewModelProvider(
                    requireActivity(),
                    RestorationWithCloudStateFactory(restorationFactory.create(storage))
                ).get(RestorationWithCloudState::class.java)
                    .restoreWallet()
                navigateToRestorationWithCloud()
            } catch (e: Exception) {
                Logger.e(e, "Error occurred during storage obtaining")
                showAuthFailedDialog()
            }
        }
    }

    private fun navigateToRestorationWithCloud() {
        (requireActivity() as WalletRestoreRouter).toBackupWithCloud()
    }

    private fun showAuthFailedDialog() {
        ErrorDialog(
            requireContext(),
            title = string(back_up_wallet_authentication_error_title),
            description = string(back_up_wallet_authentication_error_desc)
        ).show()
    }

    companion object {
        @Suppress("DEPRECATION")
        fun newInstance() = ChooseRestoreOptionFragment()

        private const val REQUEST_CODE_SIGN_IN = 222
    }

}
