package com.tari.android.wallet.ui.screen.onboarding.inroduction

import com.tari.android.wallet.application.walletManager.WalletLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.settings.allSettings.TariVersionModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class IntroductionViewModel : CommonViewModel() {

    @Inject
    lateinit var walletLauncher: WalletLauncher

    private val _uiState = MutableStateFlow(
        IntroductionModel.UiState(
            versionInfo = TariVersionModel(networkRepository).versionInfo,
            networkName = networkRepository.currentNetwork.network.displayName,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)
    }

    fun onCreateWalletClick() {
        walletLauncher.start()
    }
}