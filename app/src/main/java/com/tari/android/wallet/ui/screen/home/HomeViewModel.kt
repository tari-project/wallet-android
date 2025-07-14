package com.tari.android.wallet.ui.screen.home

import android.net.Uri
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class HomeViewModel : CommonViewModel() {

    @Inject
    lateinit var airdropRepository: AirdropRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(HomeModel.UiState(airdropLoggedIn = airdropRepository.airdropToken.value != null))
    val uiState = _uiState.asStateFlow()

    val isAuthenticated: Boolean
        get() = securityPrefRepository.isAuthenticated

    init {
        collectFlow(airdropRepository.airdropToken) { token -> _uiState.update { it.copy(airdropLoggedIn = token != null) } }

        launchOnIo {
            walletManager.doOnWalletFailed {
                showErrorDialog(it)
            }
        }
    }

    fun navigateToAuth(uri: Uri?) {
        tariNavigator.navigate(Navigation.Auth.AuthScreen(uri))
    }

    fun onMenuItemClicked(option: HomeModel.BottomMenuOption) {
        when (option) {
            HomeModel.BottomMenuOption.Shop,
            HomeModel.BottomMenuOption.Home,
            HomeModel.BottomMenuOption.Settings,
            HomeModel.BottomMenuOption.Profile -> {
                _uiState.update {
                    it.copy(selectedMenuItem = option)
                }
            }

            HomeModel.BottomMenuOption.Gem -> showNotReadyYetDialog()
        }
    }

    fun onDestroy() {
        securityPrefRepository.isAuthenticated = false
    }
}