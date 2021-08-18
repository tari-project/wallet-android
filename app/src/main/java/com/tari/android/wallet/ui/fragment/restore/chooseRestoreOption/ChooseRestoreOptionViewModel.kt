package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.backup.BackupFileIsEncryptedException
import com.tari.android.wallet.infrastructure.backup.BackupStorage
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageTamperedException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

class ChooseRestoreOptionViewModel : CommonViewModel() {

    @Inject
    lateinit var backupStorage: BackupStorage

    init {
        component?.inject(this)
    }

    private val _state = SingleLiveEvent<ChooseRestoreOptionState>()
    val state: LiveData<ChooseRestoreOptionState> = _state

    private val _navigation = SingleLiveEvent<ChooseRestoreOptionNavigation>()
    val navigation: LiveData<ChooseRestoreOptionNavigation> = _navigation

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupStorage.onSetupActivityResult(requestCode, resultCode, data)
                restoreFromBackup()
            } catch (exception: Exception) {
                Logger.e("Backup storage setup failed: $exception")
                backupStorage.signOut()
                _state.postValue(ChooseRestoreOptionState.EndProgress)
                showAuthFailedDialog()
            }
        }
    }

    private suspend fun restoreFromBackup() {
        try {
            // try to restore with no password
            backupStorage.restoreLatestBackup()
            _navigation.postValue(ChooseRestoreOptionNavigation.ToRestoreInProgress)
        } catch (exception: Exception) {
            when (exception) {
                is BackupStorageAuthRevokedException -> {
                    Logger.e(exception, "Auth revoked.")
                    backupStorage.signOut()
                    showAuthFailedDialog()
                }
                is BackupStorageTamperedException -> { // backup file not found
                    Logger.e(exception, "Backup file not found.")
                    backupStorage.signOut()
                    showBackupFileNotFoundDialog()
                }
                is BackupFileIsEncryptedException -> {
                    _navigation.postValue(ChooseRestoreOptionNavigation.ToEnterRestorePassword)
                }
                is IOException -> {
                    Logger.e(exception, "Restore failed: network connection.")
                    backupStorage.signOut()
                    showRestoreFailedDialog(resourceManager.getString(R.string.error_no_connection_title))
                }
                else -> {
                    Logger.e(exception, "Restore failed: $exception")
                    backupStorage.signOut()
                    showRestoreFailedDialog(exception.message)
                }
            }

            _state.postValue(ChooseRestoreOptionState.EndProgress)
        }
    }

    private fun showBackupFileNotFoundDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.restore_wallet_error_file_not_found),
            onClose = { _backPressed.call() })
        _errorDialag.postValue(args)
    }

    private fun showRestoreFailedDialog(message: String? = null) {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            message ?: resourceManager.getString(R.string.restore_wallet_error_desc)
        )
        _errorDialag.postValue(args)
    }

    private fun showAuthFailedDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        )
        _errorDialag.postValue(args)
    }
}