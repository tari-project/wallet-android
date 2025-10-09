package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupException
import com.tari.android.wallet.infrastructure.backup.BackupGoogleSignInFailedException
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.joda.time.DateTime
import java.net.UnknownHostException
import javax.inject.Inject

class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupPrefs: BackupPrefRepository

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    init {
        component.inject(this)
    }

    private val currentOption
        get() = backupPrefs.currentBackupOption // Currently it's always Google

    private val _uiState = MutableStateFlow(
        UiState(
            backupState = backupStateHandler.backupState.value,
            backupOption = currentOption,

            seedPhraseWarning = tariSettingsSharedRepository.hasVerifiedSeedWords,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    init {
        launchOnMain {
            backupPrefs.doOnSettingsUpdated {
                _uiState.update { it.copy(backupOption = backupPrefs.currentBackupOption) }
            }
        }

        collectFlow(backupStateHandler.backupState) { newBackupState ->
            _uiState.update { it.copy(backupState = newBackupState) }
        }

        launchOnMain {
            tariSettingsSharedRepository.doOnSettingsUpdated {
                _uiState.update { it.copy(seedPhraseWarning = tariSettingsSharedRepository.hasVerifiedSeedWords) }
            }
        }

        launchOnMain {
            backupStateHandler.doOnBackupError { backupException ->
                showBackupFailureDialog(backupException)
            }
        }
    }

    fun setupStorage(launcher: ActivityResultLauncher<Intent?>) {
        backupManager.setupStorage(launcher)
    }

    fun handleActivityResult(result: ActivityResult) {
        launchOnIo {
            try {
                val authResultHandled = backupManager.onSetupActivityResult(result)
                if (authResultHandled) {
                    backupManager.backupNow(
                        onSuccess = {
                            backupPrefs.updateOption(currentOption.copy(isEnabled = true))
                        },
                        onFailure = { backupException ->
                            backupManager.turnOff()
                            showBackupFailureDialog(backupException)
                        }
                    )
                }
            } catch (e: Throwable) {
                backupManager.turnOff()
                showBackupFailureDialog(e)
            }
        }
    }

    fun onBackupWithRecoveryPhraseClick() {
        runWithAuthorization {
            tariNavigator.navigate(Navigation.BackupSettings.ToWalletBackupWithRecoveryPhrase)
        }
    }

    fun onUpdatePassword() {
        runWithAuthorization {
            if (backupPrefs.backupPassword == null) {
                tariNavigator.navigate(Navigation.BackupSettings.ToChangePassword)
            } else {
                tariNavigator.navigate(Navigation.BackupSettings.ToConfirmPassword)
            }
        }
    }

    fun learnMore() {
        tariNavigator.navigate(Navigation.BackupSettings.ToLearnMore)
    }

    fun onBackupToCloud() = backupManager.backupNow()

    fun onBackupSwitchChecked(checked: Boolean) {
        if (checked) {
            launchOnMain { _effect.send(Effect.SetupStorage) }
        } else {
            tryToTurnOffBackup()
        }
    }

    private fun tryToTurnOffBackup() {
        showModularDialog(
            ModularDialogArgs(
                dialogArgs = DialogArgs(
                    cancelable = true,
                    canceledOnTouchOutside = false,
                ),
                modules = listOf(
                    HeadModule(resourceManager.getString(R.string.back_up_wallet_turn_off_backup_warning_title)),
                    BodyModule(resourceManager.getString(R.string.back_up_wallet_turn_off_backup_warning_description)),
                    ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                        launchOnIo {
                            try {
                                backupManager.turnOff()
                                hideDialog()
                            } catch (exception: Exception) {
                                logger.i(exception.toString())
                            }
                        }
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close) {
                        hideDialog()
                    },
                ),
            )
        )
    }

    private fun showBackupFailureDialog(exception: Throwable? = null) {
        logger.i("Backup storage setup failed: $exception")

        showSimpleDialog(
            title = when (exception) {
                is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
                else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_title)
            },
            description = when {
                exception is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_desc)
                exception is BackupException -> exception.message.orEmpty()
                exception is BackupStorageAuthRevokedException -> resourceManager.getString(R.string.check_backup_storage_status_auth_revoked_error_description)
                exception is UnknownHostException -> resourceManager.getString(R.string.error_no_connection_title)
                exception is BackupGoogleSignInFailedException -> resourceManager.getString(R.string.backup_error_google_sign_in)
                else -> resourceManager.getString(
                    R.string.back_up_wallet_backing_up_error_desc,
                    exception?.message ?: resourceManager.getString(R.string.back_up_wallet_backing_up_unknown_error),
                )
            },
        )
    }

    data class UiState(
        private val backupState: BackupState,
        private val backupOption: BackupOption,

        val seedPhraseWarning: Boolean,
    ) {
        val backupNowAvailable: Boolean
            get() = backupOption.isEnabled && backupState !is BackupState.BackupInProgress
        val showPasswordButton: Boolean
            get() = backupOption.isEnabled  // TODO review if it is correct

        val backupProgress: Boolean
            get() = backupState is BackupState.BackupInProgress
        val backupSwitchChecked: Boolean
            get() = backupOption.isEnabled
        val lastSuccessDate: DateTime?
            get() = backupOption.lastSuccessDate?.date
    }

    sealed class Effect() {
        data object SetupStorage : Effect()
    }
}
