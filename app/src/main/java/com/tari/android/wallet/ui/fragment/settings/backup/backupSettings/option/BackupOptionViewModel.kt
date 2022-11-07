package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupException
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class BackupOptionViewModel : CommonViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var backupManager: BackupManager

    private val _option = MutableLiveData<BackupOptionDto>()
    val option: LiveData<BackupOptionDto> = _option

    private val _switchChecked = MutableLiveData<Boolean>()
    val switchChecked: LiveData<Boolean> = _switchChecked

    private val _inProgress = MutableLiveData<Boolean>(false)
    val inProgress: LiveData<Boolean> = _inProgress

    private val _openFolderSelection = SingleLiveEvent<Unit>()
    val openFolderSelection: LiveData<Unit> = _openFolderSelection

    init {
        component.inject(this)
    }

    val title: Int
        get() = when (option.value!!.type) {
            BackupOptions.Google -> R.string.back_up_wallet_google_title
            BackupOptions.Local -> R.string.back_up_wallet_local_file_title
        }

    fun setup(option: BackupOptions) {
        _option.value = backupSettingsRepository.getOptionList.first { it.type == option }
        _switchChecked.value = _option.value!!.isEnable
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentOption = _option.value!!.type
            try {
                if (backupManager.onSetupActivityResult(requestCode, resultCode, data)) {
                    backupSettingsRepository.getOptionDto(currentOption)?.copy(isEnable = true)?.let { backupSettingsRepository.updateOption(it) }
                    com.tari.android.wallet.event.EventBus.backupState.publishSubject
                        .filter {
                            it.backupsStates[currentOption] is BackupState.BackupUpToDate || it.backupsStates[currentOption] is BackupState.BackupFailed
                        }.take(1)
                        .subscribe {
                            (it.backupsStates[currentOption] as? BackupState.BackupFailed)?.let { turnOff(currentOption, it.backupException) }
                        }.addTo(compositeDisposable)
                    backupManager.backupNow()
                }
            } catch (e: Throwable) {
                turnOff(currentOption, e)
            }
        }
    }

    private fun turnOff(backupOption: BackupOptions, throwable: Throwable?) {
        logger.e("Backup storage setup failed: $throwable")
        backupManager.turnOff(backupOption)
        _inProgress.postValue(false)
        _switchChecked.postValue(false)
        showBackupStorageSetupFailedDialog(throwable)
    }

    fun onBackupSwitchChecked(isChecked: Boolean) {
        _inProgress.postValue(true)
        _switchChecked.postValue(isChecked)

        if (isChecked) {
            _openFolderSelection.postValue(Unit)
        } else {
            tryToTurnOffBackup()
        }
    }

    private fun tryToTurnOffBackup() {
        val onAcceptAction = {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    backupManager.turnOff(_option.value!!.type)
                    _dismissDialog.postValue(Unit)
                } catch (exception: Exception) {
                    logger.i(exception.toString())
                }
            }
        }

        val onDismissAction = {
            _inProgress.postValue(false)
            _switchChecked.postValue(true)
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
                    _dismissDialog.value = Unit
                }
            ))

        _modularDialog.postValue(args)
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
        _modularDialog.postValue(ErrorDialogArgs(errorTitle, errorDescription) {
            _switchChecked.postValue(false)
            _inProgress.postValue(false)
        }.getModular(resourceManager))
    }

    fun onBackupStateChanged(backupState: BackupState) {
        when (backupState) {
            BackupState.BackupDisabled -> handleDisabledState()
            BackupState.BackupCheckingStorage -> handleCheckingStorageState()
            BackupState.BackupStorageCheckFailed -> handleStorageCheckFailedState()
            BackupState.BackupInProgress -> handleInProgressState()
            BackupState.BackupUpToDate -> handleUpToDateState()
            is BackupState.BackupFailed -> handleFailedState()
        }
    }

    private fun handleFailedState() {
        if (_inProgress.value!!) {
            _switchChecked.postValue(false)
        } else {
            _switchChecked.postValue(true)
        }
        _inProgress.postValue(false)
        showBackupStorageSetupFailedDialog()
    }

    private fun handleUpToDateState() {
        _inProgress.postValue(false)
        _switchChecked.postValue(true)
    }

    private fun handleInProgressState() {
        _inProgress.postValue(true)
        _switchChecked.postValue(true)
    }

    private fun handleStorageCheckFailedState() {
        _inProgress.postValue(false)
        _switchChecked.postValue(true)
    }

    private fun handleCheckingStorageState() {
        _inProgress.postValue(true)
        _switchChecked.postValue(true)
    }

    private fun handleDisabledState() {
        _inProgress.postValue(false)
        _switchChecked.postValue(false)
    }
}