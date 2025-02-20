package com.tari.android.wallet.ui.screen.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.infrastructure.ShareManager
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class HomeViewModel : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    val shareViewModel = ShareManager()

    private val _uiState = MutableStateFlow(HomeModel.UiState())
    val uiState = _uiState.asStateFlow()

    val isAuthenticated: Boolean
        get() = securityPrefRepository.isAuthenticated

    init {
        component.inject(this)

        shareViewModel.tariBluetoothServer.doOnRequiredPermissions = { permissions, action ->
            permissionManager.runWithPermission(permissions, false, action)
        }

        shareViewModel.tariBluetoothClient.doOnRequiredPermissions = { permissions, action ->
            permissionManager.runWithPermission(permissions, false, action)
        }

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

    fun processIntentDeepLink(activity: Activity, intent: Intent) {
        intent.data?.toString()?.takeIf { it.isNotEmpty() }
            ?.let { deeplinkString -> deeplinkManager.parseDeepLink(deeplinkString) }
            ?.let { deeplink ->
                deeplinkManager.execute(activity, deeplink)
            }
    }

    fun onDestroy() {
        securityPrefRepository.isAuthenticated = false
    }
}