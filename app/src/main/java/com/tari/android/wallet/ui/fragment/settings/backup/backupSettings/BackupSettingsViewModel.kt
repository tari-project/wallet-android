package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject

internal class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    lateinit var biometricAuthenticationViewModel: BiometricAuthenticationViewModel

    val backupOptionsAreVisible = MutableLiveData(true)
    val folderSelectionWasSuccessful = MutableLiveData(false)

    private val _cloudBackupStatus = SingleLiveEvent<CloudBackupStatus>()
    val cloudBackupStatus: LiveData<CloudBackupStatus> = _cloudBackupStatus

    private val _navigation = SingleLiveEvent<BackupSettingsNavigation>()
    val navigation: LiveData<BackupSettingsNavigation> = _navigation

    private val _isBackupAvailable = MutableLiveData<Boolean>()
    val isBackupAvailable: LiveData<Boolean> = _isBackupAvailable

    private val _googleDriveBackupPermissionSwitchChecked = MutableLiveData<Boolean>()
    val googleDriveBackupPermissionSwitch: LiveData<Boolean> = _googleDriveBackupPermissionSwitchChecked

    private val _inProgress = MutableLiveData<Boolean>()
    val inProgress: LiveData<Boolean> = _inProgress

    private val _openFolderSelection = SingleLiveEvent<Unit>()
    val openFolderSelection: LiveData<Unit> = _openFolderSelection

    private val _showBackupsWillBeDeletedDialog = SingleLiveEvent<BackupsWillBeDeletedDialogArgs>()
    val showBackupsWillBeDeletedDialog: LiveData<BackupsWillBeDeletedDialogArgs> = _showBackupsWillBeDeletedDialog

    private val _backupWalletToCloudEnabled = MutableLiveData<Boolean>()
    val backupWalletToCloudEnabled: LiveData<Boolean> = _backupWalletToCloudEnabled

    private val _isBackupNowEnabled = MutableLiveData<Boolean>()
    val isBackupNowEnabled: LiveData<Boolean> = _isBackupNowEnabled

    private val _backupStateChanged = MutableLiveData<Unit>()
    val backupStateChanged: LiveData<Unit> = _backupStateChanged

    private val _backupPassword = MutableLiveData<Optional<String>>()
    val backupPassword: LiveData<Optional<String>> = _backupPassword

    private val _lastSuccessfulBackupDate = MutableLiveData<Optional<DateTime>>()
    val lastSuccessfulBackupDate: LiveData<Optional<DateTime>> = _lastSuccessfulBackupDate

    private val _updatePasswordEnabled = MutableLiveData<Boolean>()
    val updatePasswordEnabled: LiveData<Boolean> = _updatePasswordEnabled

    private val _backupOptionVisibility = MutableLiveData<Boolean>()
    val backupOptionsVisibility: LiveData<Boolean> = _backupOptionVisibility

    init {
        component.inject(this)

        EventBus.backupState.subscribe(this, this::onBackupStateChanged)

        backupOptionsAreVisible.postValue(backupSettingsRepository.getOptionList.any { it.isEnable })
        _googleDriveBackupPermissionSwitchChecked.postValue(backupSettingsRepository.googleDriveOption?.isEnable ?: false)
        _backupStateChanged.postValue(Unit)
        refillSharedPrefsData()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupManager.onSetupActivityResult(requestCode, resultCode, data)
                folderSelectionWasSuccessful.postValue(true)
                _blockedBackPressed.postValue(true)
                backupManager.backup(isInitialBackup = true)
            } catch (exception: Exception) {
                Logger.e("Backup storage setup failed: $exception")
                backupManager.turnOff(deleteExistingBackups = true)
                _inProgress.postValue(false)
                _googleDriveBackupPermissionSwitchChecked.postValue(false)
                showBackupStorageSetupFailedDialog(exception)
            } finally {
                _blockedBackPressed.postValue(false)
            }
        }
    }

    fun onBackupWithRecoveryPhrase() {
        biometricAuthenticationViewModel.requireAuthorization {
            _navigation.postValue(BackupSettingsNavigation.ToWalletBackupWithRecoveryPhrase)
        }
    }

    fun onUpdatePassword() {
        biometricAuthenticationViewModel.requireAuthorization {
            if (backupSettingsRepository.backupPassword == null) {
                _navigation.postValue(BackupSettingsNavigation.ToChangePassword)
            } else {
                _navigation.postValue(BackupSettingsNavigation.ToConfirmPassword)
            }
        }
    }

    fun onBackupToCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupManager.backup(isInitialBackup = false, userTriggered = true)
            } catch (exception: Exception) {
                showBackupFailureDialog(exception)
            }
        }
    }

    fun onDropboxBackup() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupManager.backup(isInitialBackup = false, userTriggered = true)
            } catch (exception: Exception) {
                showBackupFailureDialog(exception)
            }
        }
    }


    fun onBackupPermissionSwitch(isChecked: Boolean) {
        _inProgress.postValue(true)
        _googleDriveBackupPermissionSwitchChecked.postValue(isChecked)

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
                        backupManager.turnOff(deleteExistingBackups = true)
                    } catch (exception: Exception) {
                        Logger.i(exception.toString())
                    }
                }
            }, onDismiss = {
                _inProgress.postValue(false)
                _googleDriveBackupPermissionSwitchChecked.postValue(true)
            })
        _showBackupsWillBeDeletedDialog.postValue(args)
    }

    private fun refillSharedPrefsData() {
        _backupPassword.postValue(Optional.ofNullable(backupSettingsRepository.backupPassword))
        _lastSuccessfulBackupDate.postValue(Optional.ofNullable(backupSettingsRepository.getOptionList.mapNotNull { it.lastSuccessDate }.minOrNull()))
    }

    private fun showBackupStorageSetupFailedDialog(exception: Exception? = null) {
        val errorTitle = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_title)
        }
        val errorDescription = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_desc)
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        }
        _errorDialog.postValue(ErrorDialogArgs(errorTitle, errorDescription) {
            _googleDriveBackupPermissionSwitchChecked.postValue(false)
            _inProgress.postValue(false)
        })
    }

    private fun showBackupFailureDialog(exception: Exception?) {
        val errorTitle = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
            else -> resourceManager.getString(R.string.back_up_wallet_backing_up_error_title)
        }
        val errorDescription = when {
            exception is BackupStorageFullException -> resourceManager.getString(
                R.string.backup_wallet_storage_full_desc
            )
            exception is BackupStorageAuthRevokedException -> resourceManager.getString(
                R.string.check_backup_storage_status_auth_revoked_error_description
            )
            exception is UnknownHostException -> resourceManager.getString(R.string.error_no_connection_title)
            exception?.message == null -> resourceManager.getString(R.string.back_up_wallet_backing_up_unknown_error)
            else -> resourceManager.getString(R.string.back_up_wallet_backing_up_error_desc, exception.message!!)
        }
        _errorDialog.postValue(ErrorDialogArgs(errorTitle, errorDescription))
    }

    private fun onBackupStateChanged(backupState: BackupState) {
        _backupWalletToCloudEnabled.postValue(true)
        _backupStateChanged.postValue(Unit)
        refillSharedPrefsData()
        when (backupState) {
            BackupState.BackupDisabled -> handleDisabledState()
            BackupState.BackupCheckingStorage -> handleCheckingStorageState()
            BackupState.BackupStorageCheckFailed -> handleStorageCheckFailedState()
            BackupState.BackupScheduled -> handleScheduledState()
            BackupState.BackupInProgress -> handleInProgressState()
            BackupState.BackupUpToDate -> handleUpToDateState()
            is BackupState.BackupOutOfDate -> handleOutOfDateState(backupState)
        }
    }

    private fun handleOutOfDateState(backupState: BackupState.BackupOutOfDate) {
        if (backupOptionsAreVisible.value!!) {
            if (folderSelectionWasSuccessful.value!!) {
                folderSelectionWasSuccessful.postValue(false)
                showBackupFailureDialog(backupState.backupException)
            }
            _inProgress.postValue(false)
            _googleDriveBackupPermissionSwitchChecked.postValue(true)
            _backupWalletToCloudEnabled.postValue(true)
            _isBackupAvailable.postValue(true)
            _isBackupNowEnabled.postValue(true)
            _updatePasswordEnabled.postValue(false)
            _cloudBackupStatus.postValue(
                CloudBackupStatus.Warning(
                    R.string.back_up_wallet_backup_status_outdated,
                    R.color.all_settings_back_up_status_error
                )
            )
        } else {
            showBackupStorageSetupFailedDialog()
            _inProgress.postValue(false)
            _googleDriveBackupPermissionSwitchChecked.postValue(false)
        }
    }

    private fun handleUpToDateState() {
        _inProgress.postValue(false)
        _googleDriveBackupPermissionSwitchChecked.postValue(true)
        _backupOptionVisibility.postValue(true)
        _updatePasswordEnabled.postValue(true)
        _backupWalletToCloudEnabled.postValue(false)
        _isBackupAvailable.postValue(false)
        _isBackupNowEnabled.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.Success)
    }

    private fun handleInProgressState() {
        _inProgress.postValue(true)
        _googleDriveBackupPermissionSwitchChecked.postValue(true)
        _backupWalletToCloudEnabled.postValue(false)
        _isBackupAvailable.postValue(true)
        _isBackupNowEnabled.postValue(false)
        _updatePasswordEnabled.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.InProgress(R.string.back_up_wallet_backup_status_in_progress))
    }

    private fun handleScheduledState() {
        _inProgress.postValue(false)
        _googleDriveBackupPermissionSwitchChecked.postValue(true)
        _backupWalletToCloudEnabled.postValue(true)
        _isBackupAvailable.postValue(true)
        _isBackupNowEnabled.postValue(true)
        _updatePasswordEnabled.postValue(true)
        if (backupSettingsRepository.getOptionList.all { it.lastFailureDate == null }) {
            _cloudBackupStatus.postValue(CloudBackupStatus.Scheduled)
        } else {
            _cloudBackupStatus.postValue(
                CloudBackupStatus.Warning(
                    R.string.back_up_wallet_backup_status_scheduled,
                    R.color.all_settings_back_up_status_processing
                )
            )
        }
    }

    private fun handleStorageCheckFailedState() {
        _inProgress.postValue(false)
        _googleDriveBackupPermissionSwitchChecked.postValue(true)
        _backupWalletToCloudEnabled.postValue(false)
        _isBackupAvailable.postValue(true)
        _isBackupNowEnabled.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.Warning(color = R.color.all_settings_back_up_status_error))
    }

    private fun handleCheckingStorageState() {
        _inProgress.postValue(true)
        _googleDriveBackupPermissionSwitchChecked.postValue(true)
        _backupWalletToCloudEnabled.postValue(false)
        _isBackupAvailable.postValue(true)
        _isBackupNowEnabled.postValue(false)
        _updatePasswordEnabled.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.InProgress(R.string.back_up_wallet_backup_status_checking_backup))
    }

    private fun handleDisabledState() {
        _backupOptionVisibility.postValue(false)
        _inProgress.postValue(false)
        _googleDriveBackupPermissionSwitchChecked.postValue(false)
    }
}

