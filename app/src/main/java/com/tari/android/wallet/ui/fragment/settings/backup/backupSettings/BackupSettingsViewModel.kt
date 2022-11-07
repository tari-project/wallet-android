package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.tariSettings.TariSettingsSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.net.UnknownHostException
import java.util.*
import javax.inject.Inject

class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var tariSettingsSharedRepository: TariSettingsSharedRepository

    lateinit var biometricAuthenticationViewModel: BiometricAuthenticationViewModel

    val backupOptionsAreVisible = MutableLiveData(true)
    val options = MutableLiveData<List<BackupOptionViewModel>>()

    private val _cloudBackupStatus = SingleLiveEvent<CloudBackupStatus>()
    val cloudBackupStatus: LiveData<CloudBackupStatus> = _cloudBackupStatus

    private val _navigation = SingleLiveEvent<BackupSettingsNavigation>()
    val navigation: LiveData<BackupSettingsNavigation> = _navigation

    private val _isBackupNowAvailabilityChanged = MutableLiveData<Boolean>()
    val isBackupNowAvailabilityChanged: LiveData<Boolean> = _isBackupNowAvailabilityChanged

    private val _isBackupNowEnabled = MutableLiveData<Boolean>()
    val backupNowEnabled: LiveData<Boolean> = _isBackupNowEnabled

    private val _isBackupNowAvailable = MutableLiveData<Boolean>()
    val isBackupNowAvailable: LiveData<Boolean> = _isBackupNowAvailable

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
        _backupStateChanged.postValue(Unit)
        refillSharedPrefsData()
    }

    fun setupWithOptions(optionsList: List<BackupOptionViewModel>) {
        this.options.postValue(optionsList)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                options.value.orEmpty().firstOrNull { it.option.value!!.type == backupManager.currentOption }
                    ?.onActivityResult(requestCode, resultCode, data)
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

    fun onBackupToCloud() = backupManager.backupNow()

    private fun refillSharedPrefsData() {
        _backupPassword.postValue(Optional.ofNullable(backupSettingsRepository.backupPassword))
        _lastSuccessfulBackupDate.postValue(Optional.ofNullable(backupSettingsRepository.getOptionList.mapNotNull { it.lastSuccessDate?.date }
            .minOrNull()))
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
        _modularDialog.postValue(ErrorDialogArgs(errorTitle, errorDescription).getModular(resourceManager))
    }

    private fun onBackupStateChanged(backupState: BackupsState) {
        backupState.backupsStates.forEach { state ->
            options.value.orEmpty().firstOrNull { it.option.value!!.type == state.key }?.onBackupStateChanged(state.value)
        }
        _isBackupNowEnabled.postValue(true)
        _backupStateChanged.postValue(Unit)
        refillSharedPrefsData()
        when (backupState.backupsState) {
            BackupState.BackupDisabled -> handleDisabledState()
            BackupState.BackupCheckingStorage -> handleCheckingStorageState()
            BackupState.BackupStorageCheckFailed -> handleStorageCheckFailedState()
            BackupState.BackupInProgress -> handleInProgressState()
            BackupState.BackupUpToDate -> handleUpToDateState()
            is BackupState.BackupFailed -> handleFailedState()
        }
    }

    private fun handleFailedState() {
        if (backupOptionsAreVisible.value!!) {
            _isBackupNowEnabled.postValue(true)
            _isBackupNowAvailabilityChanged.postValue(true)
            _isBackupNowAvailable.postValue(true)
            _updatePasswordEnabled.postValue(false)
            _cloudBackupStatus.postValue(
                CloudBackupStatus.Warning(R.string.back_up_wallet_backup_status_outdated, R.color.all_settings_back_up_status_error)
            )
        }
    }

    private fun handleUpToDateState() {
        _backupOptionVisibility.postValue(true)
        _updatePasswordEnabled.postValue(true)
        _isBackupNowEnabled.postValue(false)
        _isBackupNowAvailabilityChanged.postValue(false)
        _isBackupNowAvailable.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.Success)
    }

    private fun handleInProgressState() {
        _isBackupNowEnabled.postValue(false)
        _isBackupNowAvailabilityChanged.postValue(true)
        _isBackupNowAvailable.postValue(false)
        _updatePasswordEnabled.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.InProgress(R.string.back_up_wallet_backup_status_in_progress))
    }

    private fun handleStorageCheckFailedState() {
        _isBackupNowEnabled.postValue(false)
        _isBackupNowAvailabilityChanged.postValue(true)
        _isBackupNowAvailable.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.Warning(color = R.color.all_settings_back_up_status_error))
    }

    private fun handleCheckingStorageState() {
        _isBackupNowEnabled.postValue(false)
        _isBackupNowAvailabilityChanged.postValue(true)
        _isBackupNowAvailable.postValue(false)
        _updatePasswordEnabled.postValue(false)
        _cloudBackupStatus.postValue(CloudBackupStatus.InProgress(R.string.back_up_wallet_backup_status_checking_backup))
    }

    private fun handleDisabledState() {
        _backupOptionVisibility.postValue(false)
    }
}

