package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupException
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupSettingsViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionModel.Effect
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionModel.UiState
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Locale

class BackupOptionViewModel(
    val optionType: BackupOptionType,
    private val backupSettingsViewModel: BackupSettingsViewModel,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val backupManager: BackupManager,
) : CommonViewModel() {

    private val _uiState = MutableStateFlow(UiState(option = backupSettingsRepository.findOption(optionType)))
    val uiState = _uiState.asStateFlow()

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow


    val title: Int
        get() = when (optionType) {
            BackupOptionType.Google -> R.string.back_up_wallet_google_title
            BackupOptionType.Local -> R.string.back_up_wallet_local_file_title
            BackupOptionType.Dropbox -> R.string.back_up_wallet_dropbox_backup_title
        }

    fun setup() {
        onBackupStateChanged(EventBus.backupState.publishSubject.value?.backupsStates?.get(optionType)) // todo why update?
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (backupManager.onSetupActivityResult(optionType, requestCode, resultCode, data)) { // todo move to backupManager
                    backupSettingsRepository.getOptionDto(optionType).copy(isEnabled = true)?.let { backupSettingsRepository.updateOption(it) }
                    EventBus.backupState.publishSubject
                        .filter {
                            it.backupsStates[optionType] is BackupState.BackupUpToDate || it.backupsStates[optionType] is BackupState.BackupFailed
                        }
                        .take(1)
                        .subscribe {
                            (it.backupsStates[optionType] as? BackupState.BackupFailed)?.let { turnOff(optionType, it.backupException) }
                        }.addTo(compositeDisposable)
                    backupManager.backupNow()
                }
            } catch (e: Throwable) {
                turnOff(optionType, e)
            }
        }
    }

    private fun turnOff(backupOption: BackupOptionType, throwable: Throwable?) {
        logger.e("Backup storage setup failed: $throwable")
        backupManager.turnOff(backupOption)
        _uiState.update { it.stopLoading().switchOff() }
        showBackupStorageSetupFailedDialog(throwable)
    }

    fun onBackupSwitchChecked(isChecked: Boolean) {
        _uiState.update { it.startLoading().switch(isChecked) }
        backupSettingsViewModel.onOptionSelected(optionType)

        if (isChecked) {
            viewModelScope.launch {
                _effect.send(Effect.SetupStorage(backupManager, optionType))
            }
        } else {
            tryToTurnOffBackup()
        }
    }

    private fun tryToTurnOffBackup() {
        val onAcceptAction = {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    backupManager.turnOff(optionType)
                    dismissDialog.postValue(Unit)
                } catch (exception: Exception) {
                    logger.i(exception.toString())
                }
            }
        }

        val onDismissAction = {
            _uiState.update { it.stopLoading().switchOn() }
        }

        val args = ModularDialogArgs(
            DialogArgs(true, canceledOnTouchOutside = false), listOf(
                HeadModule(resourceManager.getString(R.string.back_up_wallet_turn_off_backup_warning_title)),
                BodyModule(resourceManager.getString(R.string.back_up_wallet_turn_off_backup_warning_description)),
                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                    onAcceptAction()
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close) {
                    onDismissAction()
                    dismissDialog.value = Unit
                }
            ))

        modularDialog.postValue(args)
    }

    private fun showBackupStorageSetupFailedDialog(exception: Throwable? = null) {
        val errorTitle = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_title)
        }
        val errorDescription = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_desc)
            is BackupException -> exception.message.orEmpty()
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        }
        modularDialog.postValue(ErrorDialogArgs(errorTitle, errorDescription) {
            _uiState.update { it.stopLoading().switchOff() }
        }.getModular(resourceManager))
    }

    fun onBackupStateChanged(backupState: BackupState?) {
        updateLastSuccessfulBackupDate(null)
        when (backupState) {
            is BackupState.BackupDisabled -> handleDisabledState()
            is BackupState.BackupInProgress -> handleInProgressState()
            is BackupState.BackupUpToDate -> handleUpToDateState()
            is BackupState.BackupFailed -> handleFailedState()
            else -> Unit
        }
    }

    private fun handleFailedState() {
        val shouldCheck = uiState.value.loading // todo why it depends on the loading state?
        _uiState.update { it.stopLoading().switch(shouldCheck) }
        showBackupStorageSetupFailedDialog()
    }

    private fun handleUpToDateState() {
        val currentState = backupSettingsRepository.getOptionDto(optionType)
        _uiState.update { it.stopLoading().switchOn() }
        updateLastSuccessfulBackupDate(currentState.lastSuccessDate?.date)
    }

    private fun handleInProgressState() {
        _uiState.update { it.startLoading().switchOn() }
    }

    private fun handleDisabledState() {
        _uiState.update { it.stopLoading().switchOff() }
    }

    private fun updateLastSuccessfulBackupDate(lastSuccessfulBackupDate: DateTime?) {
        val date = lastSuccessfulBackupDate?.let {
            val date = it.toLocalDateTime()
            resourceManager.getString(
                R.string.back_up_wallet_last_successful_backup,
                BACKUP_DATE_FORMATTER.print(date),
                BACKUP_TIME_FORMATTER.print(date)
            )
        } ?: ""

        _uiState.update { it.copy(lastSuccessDate = date) }
    }

    companion object {
        private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
    }
}