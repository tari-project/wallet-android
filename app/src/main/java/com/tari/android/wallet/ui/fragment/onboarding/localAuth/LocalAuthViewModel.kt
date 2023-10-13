package com.tari.android.wallet.ui.fragment.onboarding.localAuth

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class LocalAuthViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var backupManager: BackupManager

    val secureState = MutableLiveData(SecureState(true, false, false))


    init {
        component.inject(this)
        sharedPrefsWrapper.onboardingAuthSetupStarted = true
        updateState()

        sharedPrefsWrapper.updateNotifier.subscribe {
            updateState()
        }.addTo(compositeDisposable)
    }

    fun updateState() {
        secureState.value = SecureState(
            authService.isBiometricAuthAvailable,
            sharedPrefsWrapper.pinCode != null,
            sharedPrefsWrapper.biometricsAuth == true
        )
    }

    fun securedWithBiometrics() {
        sharedPrefsWrapper.biometricsAuth = true
        secureState.value = secureState.value!!.copy(biometricsSecured = true)
    }

//    fun securedWithPinCode() {
//        sharedPrefsWrapper.onboardingAuthSetupCompleted = true
//        secureState.value = secureState.value!!.copy(pinCodeSecured = true)
//    }
}