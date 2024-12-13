package com.tari.android.wallet.ui.screen.auth

import android.net.Uri
import com.tari.android.wallet.R
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.application.walletManager.WalletLauncher
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import javax.inject.Inject

class AuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var walletLauncher: WalletLauncher

    @Inject
    lateinit var migrationManager: MigrationManager

    init {
        component.inject(this)

        launchOnIo {
            migrationManager.validateVersion(
                onValid = {
                    launchOnMain {
                        walletLauncher.start()
                    }
                },
                onError = {
                    launchOnMain {
                        showIncompatibleVersionDialog()
                    }
                }
            )
        }
    }

    private fun showIncompatibleVersionDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.ffi_validation_error_title)),
            BodyModule(resourceManager.getString(R.string.ffi_validation_error_message)),
            ButtonModule(resourceManager.getString(R.string.ffi_validation_error_delete), ButtonStyle.Warning) {
                hideDialog()
                deleteWallet()
            },
            ButtonModule(resourceManager.getString(R.string.ffi_validation_error_cancel), ButtonStyle.Close) {
                walletLauncher.start()
                hideDialog()
            }
        )
    }

    private fun deleteWallet() {
        launchOnIo {
            walletManager.deleteWallet()
            tariNavigator.navigate(Navigation.SplashScreen())
        }
    }

    fun toHomeActivity(data: Uri?) {
        securityPrefRepository.isAuthenticated = true
        tariNavigator.navigate(Navigation.Home(data))
    }
}