package com.tari.android.wallet.ui.fragment.auth

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class AuthViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    init {
        component.inject(this)
    }
}