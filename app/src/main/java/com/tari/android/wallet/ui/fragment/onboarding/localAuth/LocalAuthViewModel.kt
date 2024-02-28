package com.tari.android.wallet.ui.fragment.onboarding.localAuth

import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class LocalAuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var backupManager: BackupManager

    private val _secureState = MutableStateFlow(SecureState())
    val secureState = _secureState.asStateFlow()

    init {
        component.inject(this)
        sharedPrefsRepository.onboardingAuthSetupStarted = true
        updateState()

        securityPrefRepository.updateNotifier.subscribe {
            updateState()
        }.addTo(compositeDisposable)
    }

    private fun updateState() {
        _secureState.update {
            SecureState(
                biometricsAvailable = authService.isBiometricAuthAvailable,
                pinCodeSecured = securityPrefRepository.pinCode != null,
                biometricsSecured = securityPrefRepository.biometricsAuth == true,
            )
        }
    }

    fun securedWithBiometrics() {
        securityPrefRepository.biometricsAuth = true
        _secureState.update { it.copy(biometricsSecured = true) }
    }
}