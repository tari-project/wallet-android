package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletModel.Effect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateWalletViewModel : CommonViewModel() {

    @Inject
    lateinit var corePrefRepository: CorePrefRepository

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    init {
        component.inject(this)
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