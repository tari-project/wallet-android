package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupMapState
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.screen.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.screen.settings.userAutorization.BiometricAuthenticationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    lateinit var biometricAuthenticationViewModel: BiometricAuthenticationViewModel

    val options = MutableLiveData<List<BackupOptionViewModel>>()

    private val _isBackupNowAvailable = MutableLiveData<Boolean>()
    val isBackupNowAvailable: LiveData<Boolean> = _isBackupNowAvailable

    val backupStateChanged = MutableLiveData<Unit>()

    private val _updatePasswordEnabled = MutableLiveData<Boolean>()
    val setPasswordVisible: LiveData<Boolean> = _updatePasswordEnabled

    init {
        component.inject(this)

        collectFlow(backupStateHandler.backupState) { onBackupStateChanged(it) }

        backupStateChanged.postValue(Unit)

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
            tariNavigator.navigate(Navigation.BackupSettings.ToWalletBackupWithRecoveryPhrase)
        }
    }

    fun onUpdatePassword() {
        biometricAuthenticationViewModel.requireAuthorization {
            if (backupSettingsRepository.backupPassword == null) {
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

    private fun onBackupStateChanged(backupState: BackupMapState) {
        backupState.states.forEach { state ->
            options.value.orEmpty().firstOrNull { it.option.value!!.type == state.key }?.onBackupStateChanged(state.value)
        }

        loadOptionData()

        (backupState.backupsState as? BackupState.BackupFailed)?.let { showBackupFailureDialog(it.backupException) }
    }

    private fun loadOptionData() {
        val backupState = backupStateHandler.backupState.value
        val optionsDto = backupSettingsRepository.getOptionList
        _updatePasswordEnabled.postValue(optionsDto.any { it.isEnable })
        _isBackupNowAvailable.postValue(optionsDto.any { it.isEnable } && backupState.states.all { it.value !is BackupState.BackupInProgress })
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
        showSimpleDialog(title = errorTitle, description = errorDescription)
    }
}

