package com.tari.android.wallet.ui.screen.biometrics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.tari.android.wallet.R.string.auth_biometric_prompt
import com.tari.android.wallet.R.string.auth_device_lock_code_prompt
import com.tari.android.wallet.R.string.auth_title
import com.tari.android.wallet.databinding.FragmentChangeBiometricsBinding
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationException
import com.tari.android.wallet.ui.common.CommonXmlFragment
import com.tari.android.wallet.util.extension.observe
import com.tari.android.wallet.util.extension.string
import kotlinx.coroutines.launch

class ChangeBiometricsFragment : CommonXmlFragment<FragmentChangeBiometricsBinding, ChangeBiometricsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FragmentChangeBiometricsBinding.inflate(inflater, container, false).also { ui = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    private fun initUI() {
        val viewModel: ChangeBiometricsViewModel by viewModels()
        bindViewModel(viewModel)

        ui.loadingSwitchView.setOnCheckedChangeListener {
            doAuth(it)
        }

        observe(viewModel.state) { ui.loadingSwitchView.setState(it) }
    }

    private fun doAuth(isTurningOn: Boolean = false) {
        // check whether there's at least screen lock
        if (viewModel.authService.isDeviceSecured) {
            viewModel.startAuth(isTurningOn)
            lifecycleScope.launch {
                try {
                    // prompt system authentication dialog
                    viewModel.authService.authenticate(
                        fragment = this@ChangeBiometricsFragment,
                        title = string(auth_title),
                        subtitle = if (viewModel.authService.isBiometricAuthAvailable) string(auth_biometric_prompt)
                        else string(auth_device_lock_code_prompt),
                    )
                    authSuccess(isTurningOn)
                } catch (e: BiometricAuthenticationException) {
                    authFailed(isTurningOn)
                }
            }
        } else {
            authFailed(isTurningOn)
        }
    }

    private fun authSuccess(isChecked: Boolean) {
        viewModel.authSuccessfully(isChecked)
        viewModel.stopAuth(isChecked)
    }

    private fun authFailed(isChecked: Boolean) {
        viewModel.stopAuth(!isChecked)
    }
}