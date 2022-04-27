package com.tari.android.wallet.ui.fragment.settings.allSettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R.color.*
import com.tari.android.wallet.R.drawable.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStorageAuthRevokedException
import com.tari.android.wallet.ui.common.ClipboardArgs
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRepository
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.*
import com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions.SettingsBackupOptionViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.version.SettingsVersionViewHolderItem
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

    private val backupOption = SettingsBackupOptionViewHolderItem(leftIconId = all_settings_backup_options_icon) {
        authenticationViewModel.requireAuthorization { _navigation.postValue(AllSettingsNavigation.ToBackupSettings) }
    }

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var networkRepository: NetworkRepository

    private val _navigation: SingleLiveEvent<AllSettingsNavigation> = SingleLiveEvent()
    val navigation: LiveData<AllSettingsNavigation> = _navigation

    private val _shareBugReport: SingleLiveEvent<Unit> = SingleLiveEvent()
    val shareBugReport: LiveData<Unit> = _shareBugReport

    private val _openYatOnboarding = SingleLiveEvent<Unit>()
    val openYatOnboarding: LiveData<Unit> = _openYatOnboarding

    private val _allSettingsOptions = MutableLiveData<MutableList<CommonViewHolderItem>>()
    val allSettingsOptions: LiveData<MutableList<CommonViewHolderItem>> = _allSettingsOptions

    init {
        component.inject(this)
        EventBus.backupState.subscribe(this) { backupState -> onBackupStateChanged(backupState) }
        checkStorageStatus()

        initOptions()
    }

    private fun initOptions() {
        val versionText = TariVersionModel(networkRepository).versionInfo
        val versionArgs = ClipboardArgs(
            resourceManager.getString(all_settings_version_text_copy_title), versionText,
            resourceManager.getString(all_settings_version_text_copy_toast_message)
        )

        val allOptions = mutableListOf(
            SettingsTitleDto(resourceManager.getString(all_settings_security_label)),
            backupOption,
            SettingsTitleDto(resourceManager.getString(all_settings_secondary_settings_label)),
            ButtonViewDto(resourceManager.getString(all_settings_report_a_bug), all_settings_report_bug_icon) { _shareBugReport.postValue(Unit) },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_visit_site), all_settings_visit_tari_icon) {
                _openLink.postValue(resourceManager.getString(tari_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_contribute), all_settings_contribute_to_tari_icon) {
                _openLink.postValue(resourceManager.getString(github_repo_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_user_agreement), all_settings_user_agreement_icon) {
                _openLink.postValue(resourceManager.getString(user_agreement_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_privacy_policy), all_settings_privacy_policy_icon) {
                _openLink.postValue(resourceManager.getString(privacy_policy_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_disclaimer), all_settings_disclaimer_icon) {
                _openLink.postValue(resourceManager.getString(disclaimer_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_explorer), all_settings_block_explorer_icon) {
                _openLink.postValue(resourceManager.getString(explorer_url))
            },
            SettingsTitleDto(resourceManager.getString(all_settings_yat_settings_label)),
            ButtonViewDto(resourceManager.getString(all_settings_connect_yats), all_settings_yat_icon, open_in_browser_icon) {
                _openYatOnboarding.postValue(Unit)
            },
            SettingsTitleDto(resourceManager.getString(all_settings_advanced_settings_label)),
            ButtonViewDto(resourceManager.getString(all_settings_background_service), all_settings_background_service_icon) {
                _navigation.postValue(AllSettingsNavigation.ToBackgroundService)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_bridge_configuration), all_settings_bridge_configuration_icon) {
                _navigation.postValue(AllSettingsNavigation.ToTorBridges)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_select_network), all_settings_select_network_icon) {
                _navigation.postValue(AllSettingsNavigation.ToNetworkSelection)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_select_base_node), all_settings_select_base_node_icon) {
                _navigation.postValue(AllSettingsNavigation.ToBaseNodeSelection)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_delete_wallet), all_settings_delete_button_icon, null, ButtonStyle.Warning) {
                _navigation.postValue(AllSettingsNavigation.ToDeleteWallet)
            },
            DividerViewHolderItem(),
            SettingsVersionViewHolderItem(versionText) { _copyToClipboard.postValue(versionArgs) }
        )
        _allSettingsOptions.postValue(allOptions)
    }

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
            backupOption.backupState = PresentationBackupState(Warning)
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
            backupOption.backupState = presentationBackupState
        }
        _allSettingsOptions.postValue(_allSettingsOptions.value)
    }

    private fun updateLastSuccessfulBackupDate() {
        val time = backupSettingsRepository.lastSuccessfulBackupDate?.toLocalDateTime()
        val text = if (time == null) "" else {
            resourceManager.getString(back_up_wallet_last_successful_backup, BACKUP_DATE_FORMATTER.print(time), BACKUP_TIME_FORMATTER.print(time))
        }
        backupOption.lastBackupDate = text
        _allSettingsOptions.postValue(_allSettingsOptions.value)
    }

    private fun showBackupStorageCheckFailedDialog(message: String) {
        val errorArgs = ErrorDialogArgs(resourceManager.getString(check_backup_storage_status_error_title), message)
        _modularDialog.postValue(errorArgs.getModular(resourceManager))
    }
}

