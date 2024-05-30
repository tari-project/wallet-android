package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletModel.Effect
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletModel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject

class CreateWalletViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

        viewModelScope.launch(Dispatchers.IO) {
            EventBus.torProxyState.publishSubject.asFlow().collect {
                _uiState.value = _uiState.value.copy(torState = it)
            }
        }
    }

    fun onContinueButtonClick() {
        corePrefRepository.onboardingCompleted = true
        corePrefRepository.onboardingAuthSetupStarted = true
    }

    fun waitUntilWalletCreated() {
        viewModelScope.launch(Dispatchers.IO) {
            walletStateHandler.doOnWalletRunning {
                viewModelScope.launch(Dispatchers.Main) {
                    _effect.send(Effect.StartCheckmarkAnimation)
                }
            }
        }
    }
}