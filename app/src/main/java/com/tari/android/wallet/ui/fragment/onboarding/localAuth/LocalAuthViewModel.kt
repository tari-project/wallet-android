package com.tari.android.wallet.ui.fragment.onboarding.localAuth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationType
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class LocalAuthViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var authService: BiometricAuthenticationService

    private val _authType = MutableLiveData<BiometricAuthenticationType>()
    val authType: LiveData<BiometricAuthenticationType> = _authType

    init {
        component.inject(this)
        _authType.value = authService.authType
    }
}