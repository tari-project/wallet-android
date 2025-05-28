package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import java.net.UnknownHostException
import javax.inject.Inject

class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    val optionViewModel = MutableLiveData<BackupOptionViewModel>()

    private val _isBackupNowAvailable = MutableLiveData<Boolean>()
    val isBackupNowAvailable: LiveData<Boolean> = _isBackupNowAvailable

    val backupStateChanged = MutableLiveData<Unit>()

    private val _updatePasswordEnabled = MutableLiveData<Boolean>()
    val setPasswordVisible: LiveData<Boolean> = _updatePasswordEnabled

    init {
        component.inject(this)

        collectFlow(backupStateHandler.backupState) { onBackupStateChanged(it) }

        backupStateChanged.postValue(Unit)

        optionViewModel.postValue(BackupOptionViewModel().apply { setup() })

        loadOptionData()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        launchOnIo {
            runCatching {
                optionViewModel.value?.onActivityResult(requestCode, resultCode, data)
            }.onFailure {
                logger.i("Error handling activity result: ${it.message}")
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

    private fun onBackupStateChanged(backupState: BackupState) {
        optionViewModel.value?.onBackupStateChanged(backupState)

        loadOptionData()

        if (backupState is BackupState.BackupFailed) showBackupFailureDialog(backupState.backupException)
    }

    private fun loadOptionData() {
        val currentOption = backupSettingsRepository.currentBackupOption
        _updatePasswordEnabled.postValue(currentOption.isEnable)
        _isBackupNowAvailable.postValue(currentOption.isEnable && !backupStateHandler.inProgress)
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
}

