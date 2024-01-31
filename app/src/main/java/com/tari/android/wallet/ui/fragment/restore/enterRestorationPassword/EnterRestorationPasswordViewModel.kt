package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.WalletStartFailedException
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword.EnterRestorationPasswordModel.Parameters
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.security.GeneralSecurityException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class EnterRestorationPasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    private lateinit var parameters: Parameters

    private val _state = SingleLiveEvent<EnterRestorationPasswordState>()
    val state: LiveData<EnterRestorationPasswordState> = _state

    init {
        component.inject(this)

        EventBus.walletState.publishSubject.filter { it is WalletState.Running }.subscribe {
            if (WalletUtil.walletExists(walletConfig)) {
                val dto = backupSettingsRepository.getOptionDto(parameters.selectedOptionType)!!.copy(isEnabled = true)
                backupSettingsRepository.updateOption(dto)
                backupManager.backupNow()

                navigation.postValue(Navigation.EnterRestorationPasswordNavigation.OnRestore)
            }
        }.addTo(compositeDisposable)

        EventBus.walletState.publishSubject.filter { it is WalletState.Failed }
            .map { it as WalletState.Failed }
            .debounce(300L, TimeUnit.MILLISECONDS).subscribe {
                viewModelScope.launch(Dispatchers.IO) {
                    handleRestorationFailure(WalletStartFailedException(it.exception))
                }
            }.addTo(compositeDisposable)
    }

    fun assignParameters(parameters: Parameters) {
        this.parameters = parameters
    }

    fun onBack() {
        backPressed.postValue(Unit)
        viewModelScope.launch(Dispatchers.IO) {
            backupManager.signOut(parameters.selectedOptionType)
        }
    }

    fun onRestore(password: String) {
        _state.postValue(EnterRestorationPasswordState.RestoringInProgressState)
        performRestoration(password)
    }

    private fun performRestoration(password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backupManager.restoreLatestBackup(parameters.selectedOptionType, password)
                backupSettingsRepository.backupPassword = password
                viewModelScope.launch(Dispatchers.Main) {
                    walletServiceLauncher.start()
                }
            } catch (exception: Throwable) {
                handleRestorationFailure(exception)
            }
        }
    }

    private fun handleRestorationFailure(exception: Throwable) {
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
            onClose = { backPressed.call() })
        modularDialog.postValue(args.getModular(resourceManager))
    }
}