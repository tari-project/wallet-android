package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.content.Intent
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.HexString
import com.tari.android.wallet.infrastructure.backup.BackupFileIsEncryptedException
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageTamperedException
import com.tari.android.wallet.infrastructure.backup.WalletStartFailedException
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.model.throwIf
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.dialog.error.WalletErrorArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionModel.Effect
import com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption.ChooseRestoreOptionModel.UiState
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.util.WalletUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var walletConfig: WalletConfig

    private val _effect = EffectChannelFlow<Effect>()
    val effect: Flow<Effect> = _effect.flow

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

        _uiState.update { it.copy(options = backupSettingsRepository.optionList) }

        EventBus.walletState.publishSubject.filter { it is WalletState.Running }.subscribe {
            _uiState.value.selectedOption
                ?.takeIf { WalletUtil.walletExists(walletConfig) }
                ?.let { selectedOption ->
                    backupSettingsRepository.restoredTxs
                        ?.takeIf { it.utxos.orEmpty().isEmpty() }
                        ?.let { restoredTxs ->
                            val sourceAddress = FFITariWalletAddress(HexString(restoredTxs.source))
                            val tariWalletAddress = TariWalletAddress(restoredTxs.source, sourceAddress.getEmojiId())
                            val message = resourceManager.getString(R.string.backup_restored_tx)
                            val error = WalletError()
                            walletService.restoreWithUnbindedOutputs(restoredTxs.utxos, tariWalletAddress, message, error)
                            throwIf(error)
                        }

                    val dto = backupSettingsRepository.getOptionDto(selectedOption).copy(isEnabled = true)
                    backupSettingsRepository.updateOption(dto)
                    backupManager.backupNow()

                    navigation.postValue(Navigation.ChooseRestoreOptionNavigation.OnRestoreCompleted)
                }
        }.addTo(compositeDisposable)

        EventBus.walletState.publishSubject.filter { it is WalletState.Failed }
            .map { it as WalletState.Failed }
            .debounce(300L, TimeUnit.MILLISECONDS)
            .map { it.exception }
            .subscribe { exception ->
                _uiState.value.selectedOption?.let { selectedOption ->
                    viewModelScope.launch(Dispatchers.IO) {
                        handleException(selectedOption, WalletStartFailedException(exception))
                    }
                }
            }.addTo(compositeDisposable)
    }

    fun selectBackupOption(optionType: BackupOptionType) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedOption = optionType) }
            _effect.send(Effect.SetupStorage(backupManager, optionType))
            _effect.send(Effect.BeginProgress(optionType))
        }
    }

    fun navigateToRecoveryPhrase() {
        navigation.postValue(Navigation.ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        _uiState.value.selectedOption?.let { selectedOption ->
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    if (backupManager.onSetupActivityResult(selectedOption, requestCode, resultCode, data)) {
                        restoreFromBackup(selectedOption)
                    }
                } catch (exception: Exception) {
                    logger.e("Backup storage setup for $selectedOption failed: ${exception.message}")
                    backupManager.signOut(selectedOption)
                    _effect.send(Effect.EndProgress(selectedOption))
                    showAuthFailedDialog()
                }
            }
        } ?: logger.e("SelectedOption is null")
    }

    private suspend fun restoreFromBackup(currentOption: BackupOptionType) {
        try {
            // try to restore with no password
            backupManager.restoreLatestBackup(currentOption)
            viewModelScope.launch(Dispatchers.Main) {
                walletServiceLauncher.start()
            }
        } catch (exception: Throwable) {
            handleException(currentOption, exception)
        }
    }

    private suspend fun handleException(currentOption: BackupOptionType, exception: Throwable) {
        when (exception) {
            is BackupStorageAuthRevokedException -> {
                logger.e("Auth revoked for $currentOption")
                backupManager.signOut(currentOption)
                showAuthFailedDialog()
            }

            is BackupStorageTamperedException -> { // backup file not found
                logger.e("Backup file not found for $currentOption")
                backupManager.signOut(currentOption)
                showBackupFileNotFoundDialog()
            }

            is BackupFileIsEncryptedException -> {
                navigation.postValue(Navigation.ChooseRestoreOptionNavigation.ToEnterRestorePassword(currentOption))
            }

            is WalletStartFailedException -> {
                logger.e("Restore failed for $currentOption: wallet start failed")
                viewModelScope.launch(Dispatchers.Main) {
                    walletServiceLauncher.stopAndDelete()
                }
                val cause = WalletError.createFromException(exception.cause)
                if (cause == WalletError.DatabaseDataError) {
                    showRestoreFailedDialog(resourceManager.getString(R.string.restore_wallet_error_file_not_supported))
                } else if (cause != WalletError.NoError) {
                    modularDialog.postValue(WalletErrorArgs(resourceManager, cause).getErrorArgs().getModular(resourceManager))
                } else {
                    showRestoreFailedDialog(exception.cause?.message)
                }
            }

            is IOException -> {
                logger.e("Restore failed for $currentOption: network connection")
                backupManager.signOut(currentOption)
                showRestoreFailedDialog(resourceManager.getString(R.string.error_no_connection_title))
            }

            else -> {
                logger.e("Restore failed for $currentOption")
                backupManager.signOut(currentOption)
                showRestoreFailedDialog(exception.message ?: exception.toString())
            }
        }

        _effect.send(Effect.EndProgress(currentOption))
    }

    private fun showBackupFileNotFoundDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.restore_wallet_error_file_not_found),
            onClose = { backPressed.call() })
        modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun showRestoreFailedDialog(message: String? = null) {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.restore_wallet_error_desc, message.orEmpty())
        )
        modularDialog.postValue(args.getModular(resourceManager))
    }

    private fun showAuthFailedDialog() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.restore_wallet_error_title),
            resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        )
        modularDialog.postValue(args.getModular(resourceManager))
    }
}