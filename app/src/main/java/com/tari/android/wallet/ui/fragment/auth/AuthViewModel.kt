package com.tari.android.wallet.ui.fragment.auth

import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class AuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var migrationManager: MigrationManager

    val goAuth = SingleLiveEvent<Unit>()

    init {
        component.inject(this)

        viewModelScope.launch(Dispatchers.IO) {
            migrationManager.validateVersion(
                onValid = {
                    viewModelScope.launch(Dispatchers.Main) {
                        goAuth.postValue(Unit)
                    }
                },
                onError = {
                    viewModelScope.launch(Dispatchers.Main) {
                        showIncompatibleVersionDialog()
                    }
                }
            )
        }
    }

    private fun showIncompatibleVersionDialog() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.ffi_validation_error_title)),
                BodyModule(resourceManager.getString(R.string.ffi_validation_error_message)),
                ButtonModule(resourceManager.getString(R.string.ffi_validation_error_delete), ButtonStyle.Warning) {
                    dismissDialog.postValue(Unit)
                    deleteWallet()
                },
                ButtonModule(resourceManager.getString(R.string.ffi_validation_error_cancel), ButtonStyle.Close) {
                    goAuth.postValue(Unit)
                    dismissDialog.postValue(Unit)
                }
            ))
        modularDialog.postValue(args)
    }

    private fun deleteWallet() {
        // disable CTAs
        viewModelScope.launch(Dispatchers.IO) {
            walletServiceLauncher.stopAndDelete()
            navigation.postValue(Navigation.TxListNavigation.ToSplashScreen)
        }
    }
}