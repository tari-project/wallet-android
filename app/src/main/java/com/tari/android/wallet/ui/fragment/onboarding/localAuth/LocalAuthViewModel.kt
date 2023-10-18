package com.tari.android.wallet.ui.fragment.onboarding.localAuth

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class LocalAuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var backupManager: BackupManager

    val secureState = MutableLiveData(SecureState(true, false, false))


    init {
        component.inject(this)
        sharedPrefsRepository.onboardingAuthSetupStarted = true
        updateState()

        securityPrefRepository.updateNotifier.subscribe {
            updateState()
        }.addTo(compositeDisposable)
    }

    fun updateState() {
        secureState.value = SecureState(
            authService.isBiometricAuthAvailable,
            securityPrefRepository.pinCode != null,
            securityPrefRepository.biometricsAuth == true
        )
    }

    fun securedWithBiometrics() {
        securityPrefRepository.biometricsAuth = true
        secureState.value = secureState.value!!.copy(biometricsSecured = true)
    }
}