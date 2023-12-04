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
package com.tari.android.wallet.ui.fragment.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.auth_biometric_prompt
import com.tari.android.wallet.R.string.auth_device_lock_code_prompt
import com.tari.android.wallet.R.string.auth_not_available_or_canceled_desc
import com.tari.android.wallet.R.string.auth_not_available_or_canceled_title
import com.tari.android.wallet.R.string.auth_title
import com.tari.android.wallet.data.sharedPrefs.security.LoginAttemptDto
import com.tari.android.wallet.databinding.FragmentFeatureAuthBinding
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationException
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.extension.setColor
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.pinCode.EnterPinCodeFragment
import com.tari.android.wallet.ui.fragment.pinCode.PinCodeScreenBehavior
import com.tari.android.wallet.ui.fragment.settings.allSettings.TariVersionModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FeatureAuthFragment : CommonFragment<FragmentFeatureAuthBinding, AuthViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentFeatureAuthBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: AuthViewModel by viewModels()
        bindViewModel(viewModel)

        setupUi()

        observe(viewModel.goAuth) {
            viewModel.walletServiceLauncher.start()
        }

        doAuth()
    }

    private fun setupPinCodeFragment() {
        val pinCode = viewModel.securityPrefRepository.pinCode
        if (pinCode != null) {
            val pinCodeFragment = EnterPinCodeFragment.newInstance(PinCodeScreenBehavior.FeatureAuth, pinCode)
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, pinCodeFragment)
                .commit()
        }
    }

    private fun setupUi() {
        ui.networkInfoTextView.text = TariVersionModel(viewModel.networkRepository).versionInfo
    }

    private fun doAuth() {
        try {
            setupPinCodeFragment()
        } catch (e: BiometricAuthenticationException) {
            viewModel.logger.i(e.toString() + "Authentication has failed")
        }

        showBiometricAuth()
    }

    fun showBiometricAuth() {
        if (!viewModel.authService.isBiometricAuthAvailable) return

        if (viewModel.authService.isDeviceSecured) {
            lifecycleScope.launch {
                try {
                    if (viewModel.securityPrefRepository.biometricsAuth == true) {
                        // prompt system authentication dialog
                        viewModel.authService.authenticate(
                            this@FeatureAuthFragment,
                            title = string(auth_title),
                            subtitle =
                            if (viewModel.authService.isBiometricAuthAvailable) string(auth_biometric_prompt)
                            else string(auth_device_lock_code_prompt)
                        )
                        authSuccessfully()
                    }
                } catch (e: BiometricAuthenticationException) {
                    viewModel.logger.i(e.toString() + "Authentication has failed")
                }
            }
        } else {
            displayAuthNotAvailableDialog()
        }
    }

    /**
     * Auth not available on device, i.e. lock screen is disabled
     */
    private fun displayAuthNotAvailableDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setMessage(getString(auth_not_available_or_canceled_desc))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.common_ok)) { dialog, _ -> dialog.cancel() }
            .create()
        dialog.setTitle(getString(auth_not_available_or_canceled_title))
        dialog.show()
    }

    fun authSuccessfully() {
        viewModel.securityPrefRepository.saveAttempt(LoginAttemptDto(System.currentTimeMillis(), true))
        lifecycleScope.launch(Dispatchers.Main) {
            ui.loader.visible()
            ui.progressBar.setColor(viewModel.paletteManager.getPurpleBrand(requireContext()))

            viewModel.securityPrefRepository.isFeatureAuthenticated = true
        }
    }
}