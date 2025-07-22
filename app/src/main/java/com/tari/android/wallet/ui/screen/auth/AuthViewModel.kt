package com.tari.android.wallet.ui.screen.auth

import android.net.Uri
import com.tari.android.wallet.R
import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import javax.inject.Inject

private const val PLAYSTORE_URL = "https://play.google.com/store/apps/details?id=com.tari.android.wallet"
private const val GITHUB_URL = "https://github.com/tari-project/wallet-android/releases"

class AuthViewModel : CommonViewModel() {

    @Inject
    lateinit var authService: BiometricAuthenticationService

    @Inject
    lateinit var migrationManager: MigrationManager

    init {
        component.inject(this)

        launchOnIo {
            migrationManager.validateVersion(
                onValid = {
                    launchOnMain {
                        walletManager.start()
                    }
                },
                onError = {
                    launchOnMain {
                        when (it) {
                            MigrationManager.VersionError.IncompatibleLib -> showWalletIncompatibleDialog()
                            MigrationManager.VersionError.MandatoryUpdate -> showMandatoryUpdateDialog()
                            MigrationManager.VersionError.RecommendedUpdate -> showRecommendedUpdateDialog()
                        }
                    }
                }
            )
        }
    }

    private fun showWalletIncompatibleDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.ffi_validation_error_title)),
            BodyModule(resourceManager.getString(R.string.ffi_validation_error_message)),
            ButtonModule(resourceManager.getString(R.string.ffi_validation_error_delete), ButtonStyle.Warning) {
                hideDialog()
                deleteWallet()
            },
            ButtonModule(resourceManager.getString(R.string.ffi_validation_error_cancel), ButtonStyle.Close) {
                walletManager.start()
                hideDialog()
            }
        )
    }

    private fun showMandatoryUpdateDialog() {
        showModularDialog(
            ModularDialogArgs(
                dialogArgs = DialogArgs(cancelable = false, canceledOnTouchOutside = false),
                modules = listOf(
                    HeadModule(resourceManager.getString(R.string.version_validation_error_mandatory_update_title)),
                    BodyModule(resourceManager.getString(R.string.version_validation_error_mandatory_update_message)),
                    ButtonModule(resourceManager.getString(R.string.version_validation_error_play_store), ButtonStyle.Normal) {
                        openUrl(PLAYSTORE_URL)
                    },
                    ButtonModule(resourceManager.getString(R.string.version_validation_error_github), ButtonStyle.Normal) {
                        openUrl(GITHUB_URL)
                    },
                ),
            )
        )
    }

    private fun showRecommendedUpdateDialog() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.version_validation_error_recommended_update_title)),
            BodyModule(resourceManager.getString(R.string.version_validation_error_recommended_update_message)),
            ButtonModule(resourceManager.getString(R.string.version_validation_error_play_store), ButtonStyle.Normal) {
                openUrl(PLAYSTORE_URL)
            },
            ButtonModule(resourceManager.getString(R.string.version_validation_error_github), ButtonStyle.Normal) {
                openUrl(GITHUB_URL)
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close) {
                walletManager.start()
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