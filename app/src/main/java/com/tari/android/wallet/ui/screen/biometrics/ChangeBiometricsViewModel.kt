package com.tari.android.wallet.ui.screen.biometrics

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.loadingSwitch.TariLoadingSwitchState
import javax.inject.Inject

class ChangeBiometricsViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    val state: MutableLiveData<TariLoadingSwitchState> = MutableLiveData()

    init {
        component.inject(this)
        state.value = TariLoadingSwitchState(isChecked = securityPrefRepository.biometricsAuth == true, false)
    }

    fun startAuth(isChecked: Boolean) {
        state.value = TariLoadingSwitchState(isChecked = isChecked, isLoading = true)
    }

    fun authSuccessfully(newState: Boolean) {
        securityPrefRepository.biometricsAuth = newState
    }

    fun stopAuth(isChecked: Boolean) {
        state.value = TariLoadingSwitchState(isChecked = isChecked, isLoading = false)
    }
}