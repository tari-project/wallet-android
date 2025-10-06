package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import android.content.Intent
import androidx.fragment.app.Fragment
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupException
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
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
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
            seedPhraseWarning = tariSettingsSharedRepository.hasVerifiedSeedWords,
            backupNowAvailable = currentOption.isEnable && !backupStateHandler.inProgress, // TODO review if it is correct
            showPasswordButton = currentOption.isEnable, // TODO review if it is correct

            backupSwitchChecked = currentOption.isEnable,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    init {
        collectFlow(backupStateHandler.backupState) { newBackupState ->
            _uiState.update {
                it.copy(
                    backupState = newBackupState,
                    backupNowAvailable = currentOption.isEnable && !backupStateHandler.inProgress,
                    showPasswordButton = currentOption.isEnable,

                    lastSuccessDate = currentOption.lastSuccessDate?.date,
                )
            }

            when (newBackupState) {
                is BackupState.BackupDisabled -> {
                    _uiState.update {
                        it.copy(
                            backupProgress = false,
                            backupSwitchChecked = false,
                        )
                    }
                }

                is BackupState.BackupInProgress -> {
                    _uiState.update {
                        it.copy(
                            backupProgress = true,
                            backupSwitchChecked = true,
                        )
                    }
                }

                is BackupState.BackupUpToDate -> {
                    _uiState.update {
                        it.copy(
                            backupProgress = false,
                            backupSwitchChecked = true,
                        )
                    }
                }

                is BackupState.BackupFailed -> {
                    _uiState.update {
                        it.copy(
                            backupProgress = false,
                            backupSwitchChecked = !_uiState.value.backupProgress, // TODO remove this shame
                        )
                    }
                    showBackupStorageSetupFailedDialog() // TODO limit showing error dialog
                    showBackupFailureDialog(newBackupState.backupException)
                }
            }
        }

        launchOnMain {
            tariSettingsSharedRepository.doOnSettingsUpdated {
                _uiState.update { it.copy(seedPhraseWarning = tariSettingsSharedRepository.hasVerifiedSeedWords) }
            }
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        launchOnIo {
            try {
                if (backupManager.onSetupActivityResult(requestCode, resultCode, data)) {
                    // TODO it enabled even before it's successfully backuped
                    backupPrefs.updateOption(currentOption.copy(isEnable = true))

                    collectFlow(
                        backupStateHandler.backupState
                            .filter { it is BackupState.BackupUpToDate || it is BackupState.BackupFailed }
                            .take(1)
                    ) { state ->
                        // TODO it wait's till the first backup is done. But we need to make it universal. Need to test alternative flows.
                        if (state is BackupState.BackupFailed) turnOff(state.backupException)
                    }

                    backupManager.backupNow()
                }
            } catch (e: Throwable) {
                turnOff(e)
            }
        }
    }

    fun onBackupWithRecoveryPhrase() {
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
        _uiState.update {
            it.copy(
                backupSwitchChecked = checked, // TODO check if the state is correct
                backupProgress = true,
            )
        }

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
                        _uiState.update {
                            it.copy(
                                backupProgress = false,
                                backupSwitchChecked = true,
                            )
                        }
                        hideDialog()
                    }
                ),
            )
        )
    }

    private fun turnOff(throwable: Throwable?) {
        logger.i("Backup storage setup failed: $throwable")
        backupManager.turnOff()

        _uiState.update { // TODO check if the state is correct
            it.copy(
                backupProgress = false,
                backupSwitchChecked = false,
            )
        }

        showBackupStorageSetupFailedDialog(throwable)
    }

    private fun showBackupFailureDialog(exception: Throwable?) {
        val errorTitle = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
            else -> resourceManager.getString(R.string.back_up_wallet_backing_up_error_title)
        }
        val errorDescription = when {
            exception is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_desc)
            exception is BackupStorageAuthRevokedException -> resourceManager.getString(R.string.check_backup_storage_status_auth_revoked_error_description)
            exception is UnknownHostException -> resourceManager.getString(R.string.error_no_connection_title)
            exception?.message == null -> resourceManager.getString(R.string.back_up_wallet_backing_up_unknown_error)
            else -> resourceManager.getString(R.string.back_up_wallet_backing_up_error_desc, exception.message!!)
        }
        showSimpleDialog(title = errorTitle, description = errorDescription)
    }


    private fun showBackupStorageSetupFailedDialog(exception: Throwable? = null) { // TODO merge error dialogs
        val errorTitle = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_title)
        }
        val errorDescription = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_desc)
            is BackupException -> exception.message.orEmpty()
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        }
        showSimpleDialog(
            title = errorTitle,
            description = errorDescription,
            onClose = {
                _uiState.update { // TODO check if the state is correct
                    it.copy(
                        backupSwitchChecked = false,
                        backupProgress = false,
                    )
                }
            },
        )
    }

    fun setupStorage(fragment: Fragment) {
        backupManager.setupStorage(fragment)
    }

    data class UiState(
        // TODO constructor taking currentOption
        val backupState: BackupState,
        val seedPhraseWarning: Boolean,
        val backupNowAvailable: Boolean,
        val showPasswordButton: Boolean,

        val backupProgress: Boolean = false, // TODO set real value
        val backupSwitchChecked: Boolean = false, // TODO set real value
        val lastSuccessDate: DateTime? = null,
    )

    sealed class Effect() {
        data object SetupStorage : Effect()
    }
}
