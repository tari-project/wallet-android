package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.backup.BackupStorageFullException
import com.tari.android.wallet.infrastructure.backup.BackupsState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option.BackupOptionViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.UnknownHostException
import javax.inject.Inject

class BackupSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    lateinit var biometricAuthenticationViewModel: BiometricAuthenticationViewModel

    val optionViewModels = MutableLiveData<List<BackupOptionViewModel>>()

    private val _isBackupNowAvailable = MutableLiveData<Boolean>()
    val isBackupNowAvailable: LiveData<Boolean> = _isBackupNowAvailable

    val backupStateChanged = MutableLiveData<Unit>()

    private val _updatePasswordEnabled = MutableLiveData<Boolean>()
    val setPasswordVisible: LiveData<Boolean> = _updatePasswordEnabled

    private var currentOption: BackupOptionType? = null // todo could be irrelevant if multiple backups started simultaneously

    init {
        component.inject(this)

        EventBus.backupState.subscribe(this, this::onBackupStateChanged)

        backupStateChanged.postValue(Unit)

        optionViewModels.postValue(
            backupSettingsRepository.optionList.map { option ->
                BackupOptionViewModel(
                    optionType = option.type,
                    backupSettingsViewModel = this@BackupSettingsViewModel,
                    backupManager = backupManager,
                    backupSettingsRepository = backupSettingsRepository,
                ).apply { setup() }
            }
        )

        loadOptionData()
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlin.runCatching {
                optionViewModels.value.orEmpty().firstOrNull { it.optionType == currentOption }
                    ?.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    fun onBackupWithRecoveryPhrase() {
        biometricAuthenticationViewModel.requireAuthorization {
            navigation.postValue(Navigation.BackupSettingsNavigation.ToWalletBackupWithRecoveryPhrase)
        }
    }

    fun onUpdatePassword() {
        biometricAuthenticationViewModel.requireAuthorization {
            if (backupSettingsRepository.backupPassword == null) {
                navigation.postValue(Navigation.BackupSettingsNavigation.ToChangePassword)
            } else {
                navigation.postValue(Navigation.BackupSettingsNavigation.ToConfirmPassword)
            }
        }
    }

    fun learnMore() {
        navigation.postValue(Navigation.BackupSettingsNavigation.ToLearnMore)
    }

    fun onBackupToCloud() = backupManager.backupNow()

    fun onOptionSelected(currentOption: BackupOptionType) {
        this.currentOption = currentOption
    }

    private fun onBackupStateChanged(backupState: BackupsState) {
        backupState.backupsStates.forEach { state ->
            optionViewModels.value.orEmpty().firstOrNull { it.optionType == state.key }?.onBackupStateChanged(state.value)
        }

        loadOptionData()

        (backupState.backupsState as? BackupState.BackupFailed)?.let { showBackupFailureDialog(it.backupException) }
    }

    private fun loadOptionData() {
        val backupState = EventBus.backupState.publishSubject.value
        val optionsDto = backupSettingsRepository.optionList
        _updatePasswordEnabled.postValue(optionsDto.any { it.isEnabled })
        _isBackupNowAvailable.postValue(optionsDto.any { it.isEnabled } &&
                backupState != null &&
                backupState.backupsStates.all { it.value !is BackupState.BackupInProgress })
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
        modularDialog.postValue(ErrorDialogArgs(errorTitle, errorDescription).getModular(resourceManager))
    }
}

