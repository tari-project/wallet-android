package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.BackupsWillBeDeletedDialogArgs
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class BackupOptionViewModel() : CommonViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    internal lateinit var backupManager: BackupManager

    private val _option = MutableLiveData<BackupOptionDto>()
    val option: LiveData<BackupOptionDto> = _option

    private val _switchChecked = MutableLiveData<Boolean>()
    val switchChecked: LiveData<Boolean> = _switchChecked

    private val _inProgress = MutableLiveData<Boolean>()
    val inProgress: LiveData<Boolean> = _inProgress

    private val _openFolderSelection = SingleLiveEvent<Unit>()
    val openFolderSelection: LiveData<Unit> = _openFolderSelection

    private val _showBackupsWillBeDeletedDialog = SingleLiveEvent<BackupsWillBeDeletedDialogArgs>()
    val showBackupsWillBeDeletedDialog: LiveData<BackupsWillBeDeletedDialogArgs> = _showBackupsWillBeDeletedDialog

    init {
        component.inject(this)
    }

    val title: Int
        get() = when (option.value!!.type) {
            BackupOptions.Google -> R.string.back_up_wallet_google_title
            BackupOptions.Dropbox -> R.string.back_up_wallet_dropbox_title
            BackupOptions.Local -> R.string.back_up_wallet_local_file_title
        }

    fun setup(option: BackupOptions) {
        _option.value = backupSettingsRepository.getOptionList.firstOrNull { it.type == option }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupManager.onSetupActivityResult(requestCode, resultCode, data)
//                folderSelectionWasSuccessful.postValue(true)
//                _blockedBackPressed.postValue(true)
//                backupManager.backup(isInitialBackup = true)
            } catch (exception: Exception) {
//                Logger.e("Backup storage setup failed: $exception")
//                backupManager.turnOff(deleteExistingBackups = true)
//                _inProgress.postValue(false)
//                _googleDriveBackupPermissionSwitchChecked.postValue(false)
//                showBackupStorageSetupFailedDialog(exception)
            }
        }
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
        val args = BackupsWillBeDeletedDialogArgs(
            onAccept = {
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        //todo looks not finished
                        backupManager.turnOff(_option.value!!.type, deleteExistingBackups = true)
                    } catch (exception: Exception) {
                        Logger.i(exception.toString())
                    }
                }
            }, onDismiss = {
                _inProgress.postValue(false)
                _switchChecked.postValue(true)
            })
        _showBackupsWillBeDeletedDialog.postValue(args)
    }

//    private fun onBackupStateChanged(backupState: BackupState) {
//        _backupWalletToCloudEnabled.postValue(true)
//        _backupStateChanged.postValue(Unit)
//        refillSharedPrefsData()
//        when (backupState) {
//            BackupState.BackupDisabled -> handleDisabledState()
//            BackupState.BackupCheckingStorage -> handleCheckingStorageState()
//            BackupState.BackupStorageCheckFailed -> handleStorageCheckFailedState()
//            BackupState.BackupScheduled -> handleScheduledState()
//            BackupState.BackupInProgress -> handleInProgressState()
//            BackupState.BackupUpToDate -> handleUpToDateState()
//            is BackupState.BackupOutOfDate -> handleOutOfDateState(backupState)
//        }
//    }
//
//    private fun handleOutOfDateState(backupState: BackupState.BackupOutOfDate) {
//        if (backupOptionsAreVisible.value!!) {
//            if (folderSelectionWasSuccessful.value!!) {
//                folderSelectionWasSuccessful.postValue(false)
//                showBackupFailureDialog(backupState.backupException)
//            }
//            _inProgress.postValue(false)
//            _googleDriveBackupPermissionSwitchChecked.postValue(true)
//            _backupWalletToCloudEnabled.postValue(true)
//            _isBackupAvailable.postValue(true)
//            _isBackupNowEnabled.postValue(true)
//            _updatePasswordEnabled.postValue(false)
//            _cloudBackupStatus.postValue(
//                CloudBackupStatus.Warning(
//                    R.string.back_up_wallet_backup_status_outdated,
//                    R.color.all_settings_back_up_status_error
//                )
//            )
//        } else {
//            showBackupStorageSetupFailedDialog()
//            _inProgress.postValue(false)
//            _googleDriveBackupPermissionSwitchChecked.postValue(false)
//        }
//    }
//
//    private fun handleUpToDateState() {
//        _inProgress.postValue(false)
//        _googleDriveBackupPermissionSwitchChecked.postValue(true)
//        _backupOptionVisibility.postValue(true)
//        _updatePasswordEnabled.postValue(true)
//        _backupWalletToCloudEnabled.postValue(false)
//        _isBackupAvailable.postValue(false)
//        _isBackupNowEnabled.postValue(false)
//        _cloudBackupStatus.postValue(CloudBackupStatus.Success)
//    }
//
//    private fun handleInProgressState() {
//        _inProgress.postValue(true)
//        _googleDriveBackupPermissionSwitchChecked.postValue(true)
//        _backupWalletToCloudEnabled.postValue(false)
//        _isBackupAvailable.postValue(true)
//        _isBackupNowEnabled.postValue(false)
//        _updatePasswordEnabled.postValue(false)
//        _cloudBackupStatus.postValue(CloudBackupStatus.InProgress(R.string.back_up_wallet_backup_status_in_progress))
//    }
//
//    private fun handleScheduledState() {
//        _inProgress.postValue(false)
//        _googleDriveBackupPermissionSwitchChecked.postValue(true)
//        _backupWalletToCloudEnabled.postValue(true)
//        _isBackupAvailable.postValue(true)
//        _isBackupNowEnabled.postValue(true)
//        _updatePasswordEnabled.postValue(true)
//        if (backupSettingsRepository.getOptionList.all { it.lastFailureDate == null }) {
//            _cloudBackupStatus.postValue(CloudBackupStatus.Scheduled)
//        } else {
//            _cloudBackupStatus.postValue(
//                CloudBackupStatus.Warning(
//                    R.string.back_up_wallet_backup_status_scheduled,
//                    R.color.all_settings_back_up_status_processing
//                )
//            )
//        }
//    }
//
//    private fun handleStorageCheckFailedState() {
//        _inProgress.postValue(false)
//        _googleDriveBackupPermissionSwitchChecked.postValue(true)
//        _backupWalletToCloudEnabled.postValue(false)
//        _isBackupAvailable.postValue(true)
//        _isBackupNowEnabled.postValue(false)
//        _cloudBackupStatus.postValue(CloudBackupStatus.Warning(color = R.color.all_settings_back_up_status_error))
//    }
//
//    private fun handleCheckingStorageState() {
//        _inProgress.postValue(true)
//        _googleDriveBackupPermissionSwitchChecked.postValue(true)
//        _backupWalletToCloudEnabled.postValue(false)
//        _isBackupAvailable.postValue(true)
//        _isBackupNowEnabled.postValue(false)
//        _updatePasswordEnabled.postValue(false)
//        _cloudBackupStatus.postValue(CloudBackupStatus.InProgress(R.string.back_up_wallet_backup_status_checking_backup))
//    }
//
//    private fun handleDisabledState() {
//        _backupOptionVisibility.postValue(false)
//        _inProgress.postValue(false)
//        _googleDriveBackupPermissionSwitchChecked.postValue(false)
//    }
}