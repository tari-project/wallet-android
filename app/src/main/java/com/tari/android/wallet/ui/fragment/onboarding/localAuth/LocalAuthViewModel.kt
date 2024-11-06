package com.tari.android.wallet.ui.fragment.onboarding.localAuth

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthModel.Effect
import com.tari.android.wallet.ui.fragment.onboarding.localAuth.LocalAuthModel.SecureState
import com.tari.android.wallet.ui.fragment.pinCode.PinCodeScreenBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocalAuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var backupManager: BackupManager

    private val _secureState = MutableStateFlow(SecureState())
    val secureState = _secureState.asStateFlow()

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    init {
        component.inject(this)
        sharedPrefsRepository.onboardingAuthSetupStarted = true
        updateState()

        securityPrefRepository.updateNotifier.subscribe {
            updateState()
        }.addTo(compositeDisposable)
    }

    fun securedWithBiometrics() {
        securityPrefRepository.biometricsAuth = true
        _secureState.update { it.copy(biometricsSecured = true) }
    }

    fun proceedToMain() {
        viewModelScope.launch {
            walletManager.doOnWalletRunning {
                securityPrefRepository.isAuthenticated = true
                sharedPrefsRepository.onboardingAuthSetupCompleted = true
                backupManager.backupNow()
                tariNavigator.navigate(Navigation.EnterPinCodeNavigation(PinCodeScreenBehavior.CreateConfirm))
                launchOnMain {
                    _effect.send(Effect.OnAuthSuccess)
                }
            }
        }
    }

    fun goToEnterPinCode() {
        tariNavigator.navigate(Navigation.EnterPinCodeNavigation(PinCodeScreenBehavior.Create))
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
}