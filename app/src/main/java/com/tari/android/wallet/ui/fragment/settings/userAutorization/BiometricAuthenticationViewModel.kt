package com.tari.android.wallet.ui.fragment.settings.userAutorization

import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.Fragment
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.observe
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationException
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import kotlinx.coroutines.launch
import javax.inject.Inject

//todo Remove other copy paste
class BiometricAuthenticationViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    init {
        component.inject(this)
    }

    private val _startAuthenticate = SingleLiveEvent<BiometricAuthenticateArgs>()
    private val _showAlertDialog = SingleLiveEvent<AlertDialogArgs>()

    private var onAuthorizedAction: () -> Unit = {}

    fun requireAuthorization(onAuthorized: () -> Unit) {
        onAuthorizedAction = onAuthorized
        if (authService.isDeviceSecured) {
            val subtitle = if (authService.isBiometricAuthAvailable) R.string.auth_biometric_prompt else R.string.auth_device_lock_code_prompt
            val args = BiometricAuthenticateArgs(
                resourceManager.getString(R.string.auth_title),
                resourceManager.getString(subtitle)
            )
            _startAuthenticate.postValue(args)
        } else {
            onAuthorized()
        }
    }

    private fun handleSuccess() = onAuthorizedAction.invoke()

    private fun handleError(e: Exception) {
        if (e is BiometricAuthenticationException) {
            if (e.code != BiometricPrompt.ERROR_USER_CANCELED && e.code != BiometricPrompt.ERROR_CANCELED)
                Logger.e("Other biometric error. Code: ${e.code}")
            val args = AlertDialogArgs(
                resourceManager.getString(R.string.auth_failed_desc),
                resourceManager.getString(R.string.exit),
                resourceManager.getString(R.string.auth_failed_title)
            )
            _showAlertDialog.postValue(args)
        }
    }

    companion object {
        fun bindToFragment(viewModel: BiometricAuthenticationViewModel, fragment: Fragment) = with(viewModel) {
            fragment.observe(viewModel._startAuthenticate) {
                viewModelScope.launch {
                    try {
                        authService.authenticate(fragment, title = it.title, subtitle = it.subtitle)
                        handleSuccess()
                    } catch (e: BiometricAuthenticationException) {
                        handleError(e)
                    }
                }
            }

            fragment.observe(_showAlertDialog) {
                AlertDialog.Builder(fragment.requireContext())
                    .setCancelable(false)
                    .setMessage(it.message)
                    .setNegativeButton(it.negativeButtonText) { dialog, _ -> dialog.cancel() }
                    .create()
                    .apply { setTitle(it.title) }
                    .show()
            }
        }
    }
}

