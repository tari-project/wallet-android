package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.safeCastTo
import com.tari.android.wallet.infrastructure.backup.BackupException
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOption
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Locale
import javax.inject.Inject

class BackupOptionViewModel : CommonViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    private val _option = MutableLiveData<BackupOptionDto>()
    val option: LiveData<BackupOptionDto> = _option

    private val _switchChecked = MutableLiveData<Boolean>()
    val switchChecked: LiveData<Boolean> = _switchChecked

    private val _inProgress = MutableLiveData<Boolean>(false)
    val inProgress: LiveData<Boolean> = _inProgress

    private val _openFolderSelection = SingleLiveEvent<Unit>()
    val openFolderSelection: LiveData<Unit> = _openFolderSelection

    val lastSuccessDate = MutableLiveData<String>()

    init {
        component.inject(this)
    }

    val title: Int
        get() = when (option.value!!.type) {
            BackupOption.Google -> R.string.back_up_wallet_google_title
            BackupOption.Local -> R.string.back_up_wallet_local_file_title
            BackupOption.Dropbox -> R.string.back_up_wallet_dropbox_backup_title
        }

    fun setup(option: BackupOption) {
        _option.value = backupSettingsRepository.getOptionList.first { it.type == option }
        _switchChecked.value = _option.value!!.isEnable
        onBackupStateChanged(backupStateHandler.backupState.value.states[option])
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        launchOnIo {
            val currentOption = _option.value!!.type
            try {
                if (backupManager.onSetupActivityResult(requestCode, resultCode, data)) {
                    backupSettingsRepository.getOptionDto(currentOption).copy(isEnable = true).let { backupSettingsRepository.updateOption(it) }
                    backupStateHandler.backupState
                        .filter { it.states[currentOption] is BackupState.BackupUpToDate || it.states[currentOption] is BackupState.BackupFailed }
                        .take(1)
                        .collect {
                            it.states[currentOption]?.safeCastTo<BackupState.BackupFailed>()?.let { state ->
                                turnOff(currentOption, state.backupException)
                            }
                        }
                    backupManager.backupNow()
                }
            } catch (e: Throwable) {
                turnOff(currentOption, e)
            }
        }
    }

    private fun turnOff(backupOption: BackupOption, throwable: Throwable?) {
        logger.i("Backup storage setup failed: $throwable")
        backupManager.turnOff(backupOption)
        _inProgress.postValue(false)
        _switchChecked.postValue(false)
        showBackupStorageSetupFailedDialog(throwable)
    }

    fun onBackupSwitchChecked(isChecked: Boolean) {
        _inProgress.postValue(true)
        _switchChecked.postValue(isChecked)

        if (isChecked) {
            _openFolderSelection.postValue(Unit)
        } else {
            tryToTurnOffBackup()
        }
    }

    private fun tryToTurnOffBackup() {
        val onAcceptAction = {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    backupManager.turnOff(_option.value!!.type)
                    hideDialog()
                } catch (exception: Exception) {
                    logger.i(exception.toString())
                }
            }
        }

        val onDismissAction = {
            _inProgress.postValue(false)
            _switchChecked.postValue(true)
        }

        showModularDialog(
            ModularDialogArgs(
                DialogArgs(true, canceledOnTouchOutside = false), listOf(
                    HeadModule(resourceManager.getString(R.string.back_up_wallet_turn_off_backup_warning_title)),
                    BodyModule(resourceManager.getString(R.string.back_up_wallet_turn_off_backup_warning_description)),
                    ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                        onAcceptAction()
                    },
                    ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close) {
                        onDismissAction()
                        hideDialog()
                    }
                )
            )
        )
    }

    private fun showBackupStorageSetupFailedDialog(exception: Throwable? = null) {
        val errorTitle = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_title)
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_title)
        }
        val errorDescription = when (exception) {
            is BackupStorageFullException -> resourceManager.getString(R.string.backup_wallet_storage_full_desc)
            is BackupException -> exception.message.orEmpty()
            else -> resourceManager.getString(R.string.back_up_wallet_storage_setup_error_desc)
        }
        showSimpleDialog(
            title = errorTitle,
            description = errorDescription,
            onClose = {
                _switchChecked.postValue(false)
                _inProgress.postValue(false)
            },
        )
    }

    fun onBackupStateChanged(backupState: BackupState?) {
        updateLastSuccessfulBackupDate(null)
        when (backupState) {
            BackupState.BackupDisabled -> handleDisabledState()
            BackupState.BackupInProgress -> handleInProgressState()
            BackupState.BackupUpToDate -> handleUpToDateState()
            is BackupState.BackupFailed -> handleFailedState()
            else -> Unit
        }
    }

    private fun handleFailedState() {
        if (_inProgress.value!!) {
            _switchChecked.postValue(false)
        } else {
            _switchChecked.postValue(true)
        }
        _inProgress.postValue(false)
        showBackupStorageSetupFailedDialog()
    }

    private fun handleUpToDateState() {
        val currentState = backupSettingsRepository.getOptionDto(_option.value!!.type)
        _inProgress.postValue(false)
        _switchChecked.postValue(true)
        updateLastSuccessfulBackupDate(currentState.lastSuccessDate?.date)
    }

    private fun handleInProgressState() {
        _inProgress.postValue(true)
        _switchChecked.postValue(true)
    }

    private fun handleDisabledState() {
        _inProgress.postValue(false)
        _switchChecked.postValue(false)
    }

    private fun updateLastSuccessfulBackupDate(lastSuccessfulBackupDate: DateTime?) {
        val date = lastSuccessfulBackupDate?.let {
            val date = it.toLocalDateTime()
            resourceManager.getString(
                R.string.back_up_wallet_last_successful_backup,
                BACKUP_DATE_FORMATTER.print(date),
                BACKUP_TIME_FORMATTER.print(date)
            )
        } ?: ""
        lastSuccessDate.postValue(date)
    }

    companion object {
        private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
    }
}