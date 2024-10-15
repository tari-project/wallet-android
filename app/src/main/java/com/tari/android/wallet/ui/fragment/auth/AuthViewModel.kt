package com.tari.android.wallet.ui.fragment.auth

import com.tari.android.wallet.R
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import javax.inject.Inject

class AuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var migrationManager: MigrationManager

    init {
        component.inject(this)

        launchOnIo {
            migrationManager.validateVersion(
                onValid = {
                    launchOnMain {
                        walletServiceLauncher.start()
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
                walletServiceLauncher.start()
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
}