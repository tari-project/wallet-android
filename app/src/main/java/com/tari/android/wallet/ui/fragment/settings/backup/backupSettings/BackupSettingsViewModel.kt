package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
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
import java.net.UnknownHostException
import javax.inject.Inject

class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    lateinit var biometricAuthenticationViewModel: BiometricAuthenticationViewModel

    val options = MutableLiveData<List<BackupOptionViewModel>>()

    private val _navigation = SingleLiveEvent<BackupSettingsNavigation>()
    val navigation: LiveData<BackupSettingsNavigation> = _navigation

    private val _isBackupNowAvailable = MutableLiveData<Boolean>()
    val isBackupNowAvailable: LiveData<Boolean> = _isBackupNowAvailable

    private val _backupStateChanged = MutableLiveData<Unit>()
    val backupStateChanged: LiveData<Unit> = _backupStateChanged

    private val _updatePasswordEnabled = MutableLiveData<Boolean>()
    val setPasswordVisible: LiveData<Boolean> = _updatePasswordEnabled

    init {
        component.inject(this)

        EventBus.backupState.subscribe(this, this::onBackupStateChanged)

        _backupStateChanged.postValue(Unit)

        options.postValue(backupSettingsRepository.getOptionList.map { option -> BackupOptionViewModel().apply { setup(option.type) } })

        loadOptionData()
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

    private fun onBackupStateChanged(backupState: BackupsState) {
        backupState.backupsStates.forEach { state ->
            options.value.orEmpty().firstOrNull { it.option.value!!.type == state.key }?.onBackupStateChanged(state.value)
        }

        loadOptionData()

        (backupState.backupsState as? BackupState.BackupFailed)?.let { showBackupFailureDialog(it.backupException) }
    }

    private fun loadOptionData() {
        val backupState = EventBus.backupState.publishSubject.value
        val optionsDto = backupSettingsRepository.getOptionList
        _updatePasswordEnabled.postValue(optionsDto.any { it.isEnable })
        _isBackupNowAvailable.postValue(optionsDto.any { it.isEnable } &&
                backupState != null &&
                backupState.backupsStates.all { it.value !is BackupState.BackupInProgress })
    }

    private fun showBackupFailureDialog(exception: Throwable?) {
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
}

