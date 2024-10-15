package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.onboarding.createWallet.CreateWalletModel.Effect
import kotlinx.coroutines.flow.Flow
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
        launchOnIo {
            walletManager.doOnWalletRunning {
                launchOnMain {
                    _effect.send(Effect.StartCheckmarkAnimation)
                }
            }
        }
    }
}