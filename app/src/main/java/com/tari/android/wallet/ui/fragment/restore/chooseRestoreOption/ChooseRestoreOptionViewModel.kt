package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
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
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.qr.QrScannerActivity
import com.tari.android.wallet.ui.fragment.qr.QrScannerSource
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions
import com.tari.android.wallet.util.WalletUtil
import java.io.IOException
import javax.inject.Inject

class ChooseRestoreOptionViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupPrefRepository: BackupPrefRepository

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    @Inject
    lateinit var walletConfig: WalletConfig

    private val _state = SingleLiveEvent<ChooseRestoreOptionState>()
    val state: LiveData<ChooseRestoreOptionState> = _state

    val options = MutableLiveData<List<BackupOptionDto>>()

    init {
        component.inject(this)

        options.postValue(backupPrefRepository.getOptionList)

        launchOnIo {
            walletManager.doOnWalletRunning {
                if (WalletUtil.walletExists(walletConfig) && state.value != null) {
                    backupPrefRepository.restoredTxs?.let {
                        if (it.utxos.isEmpty()) return@let

                        val tariWalletAddress = TariWalletAddress.fromBase58(it.sourceBase58)
                        val message = resourceManager.getString(R.string.backup_restored_tx)
                        val error = WalletError()
                        walletService.restoreWithUnbindedOutputs(it.utxos, tariWalletAddress, message, error)
                        throwIf(error)
                    }

                    val dto = backupPrefRepository.getOptionDto(state.value!!.backupOptions)!!.copy(isEnable = true)
                    backupPrefRepository.updateOption(dto)
                    backupManager.backupNow()

                    navigation.postValue(Navigation.ChooseRestoreOptionNavigation.OnRestoreCompleted)
                }
            }
        }

        launchOnIo {
            walletManager.doOnWalletFailed {
                handleException(WalletStartFailedException(it))
            }
        }
    }

    fun startRecovery(options: BackupOptions, hostFragment: Fragment) {
        _state.postValue(ChooseRestoreOptionState.BeginProgress(options))
        backupManager.setupStorage(options, hostFragment)
    }

    fun onRecoveryPhraseClicked() {
        tariNavigator.navigate(Navigation.ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase(seedWords = null))
    }

    fun onPaperWalletClicked(fragment: Fragment) {
        QrScannerActivity.startScanner(fragment, QrScannerSource.PaperWallet)
    }

    fun handleDeeplink(qrDeepLink: DeepLink) {
        if (qrDeepLink is DeepLink.PaperWallet) {
            showPaperWalletDialog(qrDeepLink.seedWords)
        } else {
            showInvalidQrDialog()
        }
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        launchOnIo {
            try {
                if (backupManager.onSetupActivityResult(requestCode, resultCode, data)) {
                    restoreFromBackup()
                }
            } catch (exception: Exception) {
                logger.i(exception.message + "Backup storage setup failed")
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
            launchOnMain {
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
                navigation.postValue(Navigation.ChooseRestoreOptionNavigation.ToEnterRestorePassword)
            }

            is WalletStartFailedException -> {
                logger.i("Restore failed: wallet start failed")
                launchOnMain {
                    walletServiceLauncher.stopAndDelete()
                }
                val cause = WalletError(exception.cause)
                if (cause == WalletError.DatabaseDataError) {
                    showRestoreFailedDialog(resourceManager.getString(R.string.restore_wallet_error_file_not_supported))
                } else if (cause != WalletError.NoError) {
                    showErrorDialog(cause)
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

    private fun showPaperWalletDialog(seedWords: List<String>) {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_title)),
            BodyModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_body)),
            ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_restore_button), ButtonStyle.Normal) {
                hideDialog()
                tariNavigator.navigate(Navigation.ChooseRestoreOptionNavigation.ToRestoreWithRecoveryPhrase(seedWords))
            },
            ButtonModule(resourceManager.getString(R.string.restore_wallet_paper_wallet_do_not_restore_button), ButtonStyle.Close),
        )
    }

    private fun showBackupFileNotFoundDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.restore_wallet_error_title),
            description = resourceManager.getString(R.string.restore_wallet_error_file_not_found),
            onClose = { backPressed.call() },
        )
    }

    private fun showRestoreFailedDialog(message: String? = null) {
        showSimpleDialog(
            title = resourceManager.getString(R.string.restore_wallet_error_title),
            description = resourceManager.getString(R.string.restore_wallet_error_desc, message.orEmpty()),
        )
    }

    private fun showAuthFailedDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.restore_wallet_error_title),
            description = resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc),
        )
    }

    private fun showInvalidQrDialog() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.restore_wallet_invalid_qr_code),
            description = resourceManager.getString(R.string.restore_wallet_invalid_qr_code_description),
        )
    }
}