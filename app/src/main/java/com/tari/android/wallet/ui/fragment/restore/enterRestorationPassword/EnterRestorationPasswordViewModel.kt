package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.backup.BackupStorage
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException
import javax.inject.Inject

class EnterRestorationPasswordViewModel() : CommonViewModel() {
    @Inject
    lateinit var backupStorage: BackupStorage

    init {
        component?.inject(this)
    }

    private val _state = SingleLiveEvent<EnterRestorationPasswordState>()
    val state: LiveData<EnterRestorationPasswordState> = _state

    private val _navigation = SingleLiveEvent<EnterRestorationPasswordNavigation>()
    val navigation: LiveData<EnterRestorationPasswordNavigation> = _navigation

    fun onBack() {
        _backPressed.call()
        viewModelScope.launch(Dispatchers.IO) {
            backupStorage.signOut()
        }
    }

    fun onRestore(password: String) {
        _state.postValue(EnterRestorationPasswordState.RestoringInProgressState)
        performRestoration(password)
    }

    private fun performRestoration(password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupStorage.restoreLatestBackup(password)
                _navigation.postValue(EnterRestorationPasswordNavigation.ToRestoreInProgress)
            } catch (exception: Exception) {
                handleRestorationFailure(exception)
            }
        }
    }

    private fun handleRestorationFailure(exception: Exception) {
        exception.cause?.let {
            if (it is GeneralSecurityException) {
                _state.postValue(EnterRestorationPasswordState.WrongPasswordErrorState)
                return
            }
        }
        when (exception) {
            is GeneralSecurityException -> _state.postValue(EnterRestorationPasswordState.WrongPasswordErrorState)
            else -> showUnrecoverableExceptionDialog(exception.message ?: resourceManager.getString(R.string.common_unknown_error))
        }
    }

    private fun showUnrecoverableExceptionDialog(message: String) {
        val args = ErrorDialogArgs(title = resourceManager.getString(R.string.restore_wallet_error_title),
            description = message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = { _backPressed.call() })
        _errorDialag.postValue(args)
    }
}