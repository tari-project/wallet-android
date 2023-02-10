package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.content.Intent
import androidx.lifecycle.*
import com.tari.android.wallet.R
import com.tari.android.wallet.application.*
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.error.WalletErrorArgs
import com.tari.android.wallet.ui.fragment.settings.backup.data.*
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ChooseRestoreOptionViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var walletManager: WalletManager

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var walletConfig: WalletConfig

    private val _state = SingleLiveEvent<ChooseRestoreOptionState>()
    val state: LiveData<ChooseRestoreOptionState> = _state

    private val _navigation = SingleLiveEvent<ChooseRestoreOptionNavigation>()
    val navigation: LiveData<ChooseRestoreOptionNavigation> = _navigation

    val options = MutableLiveData<List<BackupOptionDto>>()

    val migrationManager = MigrationManager()

    init {
        component.inject(this)

        options.postValue(backupSettingsRepository.getOptionList)

        EventBus.walletState.publishSubject.filter { it is WalletState.Running }.subscribe {
            if (WalletUtil.walletExists(walletConfig) && state.value != null) {
                backupSettingsRepository.restoredTxs?.let {
                    if (it.utxos.orEmpty().isEmpty()) return@let

                    val sourceAddress = FFITariWalletAddress(HexString(it.source))
                    val tariWalletAddress = TariWalletAddress(it.source, sourceAddress.getEmojiId())
                    val message = resourceManager.getString(R.string.backup_restored_tx)
                    val error = WalletError()
                    walletService.restoreWithUnbindedOutputs(it.utxos, tariWalletAddress, message, error)
                    throwIf(error)
                }
                migrationManager.updateWalletVersion()

                val dto = backupSettingsRepository.getOptionDto(state.value!!.backupOptions)!!.copy(isEnable = true)
                backupSettingsRepository.updateOption(dto)
                backupManager.backupNow()

                _navigation.postValue(ChooseRestoreOptionNavigation.OnRestoreCompleted)
            }
        }.addTo(compositeDisposable)

        EventBus.walletState.publishSubject.filter { it is WalletState.Failed }
            .map { it as WalletState.Failed }
            .debounce(300L, TimeUnit.MILLISECONDS).subscribe {
                viewModelScope.launch(Dispatchers.IO) {
                    handleException(WalletStartFailedException(it.exception))
                }
            }.addTo(compositeDisposable)
    }

    fun startRestore(options: BackupOptions) {
        _state.postValue(ChooseRestoreOptionState.BeginProgress(options))
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (backupManager.onSetupActivityResult(requestCode, resultCode, data)) {
                    restoreFromBackup()
                }
            } catch (exception: Exception) {
                logger.e(exception, "Backup storage setup failed")
                backupManager.signOut()
                _state.postValue(ChooseRestoreOptionState.EndProgress(backupManager.currentOption!!))
                showAuthFailedDialog()
            }
        }
    }

    private suspend fun restoreFromBackup() {
        try {
            // try to restore with no password
            backupManager.restoreLatestBackup()
            viewModelScope.launch(Dispatchers.Main) {
                walletServiceLauncher.start()
            }
        } catch (exception: Throwable) {
            handleException(exception)
        }
    }

    private suspend fun handleException(exception: Throwable) {
        when (exception) {
            is BackupStorageAuthRevokedException -> {
                logger.i("Auth revoked")
                backupManager.signOut()
                showAuthFailedDialog()
            }

            is BackupStorageTamperedException -> { // backup file not found
                logger.i("Backup file not found")
                backupManager.signOut()
                showBackupFileNotFoundDialog()
            }

            is BackupFileIsEncryptedException -> {
                _navigation.postValue(ChooseRestoreOptionNavigation.ToEnterRestorePassword)
            }

            is WalletStartFailedException -> {
                logger.i("Restore failed: wallet start failed")
                viewModelScope.launch(Dispatchers.Main) {
                    walletServiceLauncher.stopAndDelete()
                }
                val cause = WalletError.createFromException(exception.cause)
                if (cause == WalletError.DatabaseDataError) {
                    showRestoreFailedDialog(resourceManager.getString(R.string.restore_wallet_error_file_not_supported))
                } else if (cause != WalletError.NoError) {
                    _modularDialog.postValue(WalletErrorArgs(resourceManager, cause).getErrorArgs().getModular(resourceManager))
                } else {
                    showRestoreFailedDialog(exception.cause?.message)
                }
            }

            is IOException -> {
                logger.i("Restore failed: network connection")
                backupManager.signOut()
                showRestoreFailedDialog(resourceManager.getString(R.string.error_no_connection_title))
            }

            else -> {
                logger.i("Restore failed")
                backupManager.signOut()
                showRestoreFailedDialog(exception.message ?: exception.toString())
            }
        }

        _state.postValue(ChooseRestoreOptionState.EndProgress(backupManager.currentOption!!))
    }

    private fun showBackupFileNotFoundDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.restore_wallet_error_file_not_found),
            onClose = { _backPressed.call() })
        _modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun showRestoreFailedDialog(message: String? = null) {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.restore_wallet_error_desc, message.orEmpty())
        )
        _modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun showAuthFailedDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        )
        _modularDialog.postValue(args.getModular(resourceManager))
    }
}