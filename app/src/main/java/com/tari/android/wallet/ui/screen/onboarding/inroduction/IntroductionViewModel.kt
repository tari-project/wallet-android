package com.tari.android.wallet.ui.screen.onboarding.inroduction

import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.settings.allSettings.TariVersionModel
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class IntroductionViewModel : CommonViewModel() {

    private val _uiState = MutableStateFlow(
        IntroductionModel.UiState(
            versionInfo = TariVersionModel(networkRepository).versionInfo,
            networkName = networkRepository.currentNetwork.network.displayName,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<IntroductionModel.Effect>()
    val effect = _effect.flow

    init {
        component.inject(this)
    }

    fun onCreateWalletClick() {
        walletManager.start()
        launchOnMain {
            _effect.send(IntroductionModel.Effect.GoToCreateWallet)
        }
    }

    fun onWalletRestoreClick() {
        tariNavigator.navigate(Navigation.Restore.WalletRestoreActivity)
    }
}