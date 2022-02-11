package com.tari.android.wallet.ui.fragment.settings.allSettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.infrastructure.security.biometric.BiometricAuthenticationService
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.backup.BackupSettingsRepository
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.*
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.joda.time.format.DateTimeFormat
import java.io.IOException
import java.util.*
import javax.inject.Inject

internal class AllSettingsViewModel : CommonViewModel() {

    companion object {
        private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
        private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")
    }

    lateinit var authenticationViewModel: BiometricAuthenticationViewModel

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var authService: BiometricAuthenticationService

    private val _navigation: SingleLiveEvent<AllSettingsNavigation> = SingleLiveEvent()
    val navigation: LiveData<AllSettingsNavigation> = _navigation

    private val _shareBugReport: SingleLiveEvent<Unit> = SingleLiveEvent()
    val shareBugReport: LiveData<Unit> = _shareBugReport

    private val _backupState: MutableLiveData<PresentationBackupState> = MutableLiveData()
    val backupState: LiveData<PresentationBackupState> = _backupState

    private val _lastBackupDate: MutableLiveData<String> = MutableLiveData()
    val lastBackupDate: LiveData<String> = _lastBackupDate

    private val _versionInfo: MutableLiveData<String> = MutableLiveData()
    var versionInfo: LiveData<String> = _versionInfo

    init {
        component.inject(this)
        EventBus.backupState.subscribe(this) { backupState -> onBackupStateChanged(backupState) }
        checkStorageStatus()

        _versionInfo.postValue(TariVersionModel(networkRepository).versionInfo)
    }

    fun openTariUrl() = _openLink.postValue(resourceManager.getString(tari_url))

    fun openGithubUrl() = _openLink.postValue(resourceManager.getString(github_repo_url))

    fun openAgreementUrl() = _openLink.postValue(resourceManager.getString(user_agreement_url))

    fun openPrivateUrl() = _openLink.postValue(resourceManager.getString(privacy_policy_url))

    fun openDisclaimerUrl() = _openLink.postValue(resourceManager.getString(disclaimer_url))

    fun openExplorerUrl() = _openLink.postValue(resourceManager.getString(explorer_url))

    fun navigateToDeleteWallet() = _navigation.postValue(AllSettingsNavigation.ToDeleteWallet)

    fun navigateToBackgroundServiceSettings() = _navigation.postValue(AllSettingsNavigation.ToBackgroundService)

    fun navigateToBaseNodeSelection() = _navigation.postValue(AllSettingsNavigation.ToBaseNodeSelection)

    fun navigateToNetworkSelection() = _navigation.postValue(AllSettingsNavigation.ToNetworkSelection)

    fun navigateToBackupSettings() = authenticationViewModel.requireAuthorization { _navigation.postValue(AllSettingsNavigation.ToBackupSettings) }

    fun shareBugReport() = _shareBugReport.postValue(Unit)

    private fun checkStorageStatus() = viewModelScope.launch(Dispatchers.IO) {
        try {
            backupManager.checkStorageStatus()
        } catch (e: BackupStorageAuthRevokedException) {
            Logger.e("Backup storage auth error.")
            // show access revoked information
            showBackupStorageCheckFailedDialog(resourceManager.getString(check_backup_storage_status_auth_revoked_error_description))
        } catch (e: IOException) {
            Logger.e("Backup storage I/O (access) error.")
            showBackupStorageCheckFailedDialog(resourceManager.getString(check_backup_storage_status_access_error_description))
        } catch (e: Exception) {
            Logger.e("Backup storage tampered.")
            updateLastSuccessfulBackupDate()
        }
    }

    private fun onBackupStateChanged(backupState: BackupState?) {
        if (backupState == null) {
            _backupState.postValue(PresentationBackupState(Warning))
        } else {
            updateLastSuccessfulBackupDate()
            val presentationBackupState = when (backupState) {
                is BackupState.BackupDisabled -> PresentationBackupState(Warning)
                is BackupState.BackupCheckingStorage -> {
                    PresentationBackupState(InProgress, back_up_wallet_backup_status_checking_backup, all_settings_back_up_status_error)
                }
                is BackupState.BackupStorageCheckFailed -> PresentationBackupState(InProgress, -1, all_settings_back_up_status_error)
                is BackupState.BackupScheduled -> {
                    if (backupSettingsRepository.backupFailureDate == null) {
                        PresentationBackupState(Scheduled, back_up_wallet_backup_status_scheduled, all_settings_back_up_status_scheduled)
                    } else {
                        PresentationBackupState(Warning, back_up_wallet_backup_status_scheduled, all_settings_back_up_status_processing)
                    }
                }
                is BackupState.BackupInProgress -> {
                    PresentationBackupState(InProgress, back_up_wallet_backup_status_in_progress, all_settings_back_up_status_processing)
                }
                is BackupState.BackupUpToDate -> {
                    PresentationBackupState(Success, back_up_wallet_backup_status_up_to_date, all_settings_back_up_status_up_to_date)
                }
                is BackupState.BackupOutOfDate -> {
                    PresentationBackupState(Warning, back_up_wallet_backup_status_outdated, all_settings_back_up_status_error)
                }
            }
            _backupState.postValue(presentationBackupState)
        }
    }

    private fun updateLastSuccessfulBackupDate() {
        val time = backupSettingsRepository.lastSuccessfulBackupDate?.toLocalDateTime()
        val text = if (time == null) "" else {
            resourceManager.getString(back_up_wallet_last_successful_backup, BACKUP_DATE_FORMATTER.print(time), BACKUP_TIME_FORMATTER.print(time))
        }
        _lastBackupDate.postValue(text)
    }

    private fun showBackupStorageCheckFailedDialog(message: String) {
        val errorArgs = ErrorDialogArgs(resourceManager.getString(check_backup_storage_status_error_title), message)
        _errorDialog.postValue(errorArgs)
    }
}

