package com.tari.android.wallet.ui.fragment.restore.enterRestorationPassword

import androidx.lifecycle.LiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.WalletStartFailedException
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.SimpleDialogArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import java.security.GeneralSecurityException
import javax.inject.Inject

class EnterRestorationPasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    init {
        component.inject(this)

        launchOnIo {
            walletManager.doOnWalletRunning {
                if (walletConfig.walletExists()) {
                    val dto = backupSettingsRepository.getOptionDto(backupManager.currentOption!!)!!.copy(isEnable = true)
                    backupSettingsRepository.updateOption(dto)
                    backupManager.backupNow()

                    walletManager.onWalletRestored()
                    tariNavigator.navigate(Navigation.SplashScreen(clearTop = false))
                }
            }
        }

        launchOnIo {
            walletManager.doOnWalletFailed {
                handleRestorationFailure(WalletStartFailedException(it))
            }
        }
    }

    private val _state = SingleLiveEvent<EnterRestorationPasswordState>()
    val state: LiveData<EnterRestorationPasswordState> = _state

    fun onBack() {
        backPressed.postValue(Unit)
        launchOnIo {
            backupManager.signOut()
        }
    }

    fun onRestore(password: String) {
        _state.postValue(EnterRestorationPasswordState.RestoringInProgressState)
        performRestoration(password)
    }

    private fun performRestoration(password: String) {
        launchOnIo {
            try {
                backupManager.restoreLatestBackup(password)
                backupSettingsRepository.backupPassword = password
                launchOnMain {
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
        val args = SimpleDialogArgs(title = resourceManager.getString(R.string.restore_wallet_error_title),
            description = message,
            cancelable = false,
            canceledOnTouchOutside = false,
            onClose = { backPressed.call() })
        showModularDialog(args.getModular(resourceManager))
    }
}