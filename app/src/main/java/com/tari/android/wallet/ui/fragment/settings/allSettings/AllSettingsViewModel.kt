package com.tari.android.wallet.ui.fragment.settings.allSettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.drawable.*
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.ui.common.*
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.*
import com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions.SettingsBackupOptionViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.version.SettingsVersionViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import com.tari.android.wallet.yat.YatAdapter
import javax.inject.Inject

class AllSettingsViewModel : CommonViewModel() {

    lateinit var authenticationViewModel: BiometricAuthenticationViewModel

    private val backupOption = SettingsBackupOptionViewHolderItem(leftIconId = vector_all_settings_backup_options_icon) {
        authenticationViewModel.requireAuthorization { _navigation.postValue(AllSettingsNavigation.ToBackupSettings) }
    }

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    @Inject
    lateinit var backupManager: BackupManager

    private val _navigation: SingleLiveEvent<AllSettingsNavigation> = SingleLiveEvent()
    val navigation: LiveData<AllSettingsNavigation> = _navigation

    private val _openYatOnboarding = SingleLiveEvent<Unit>()
    val openYatOnboarding: LiveData<Unit> = _openYatOnboarding

    private val _allSettingsOptions = MutableLiveData<MutableList<CommonViewHolderItem>>()
    val allSettingsOptions: LiveData<MutableList<CommonViewHolderItem>> = _allSettingsOptions

    init {
        component.inject(this)
        initOptions()
        EventBus.backupState.subscribe(this) { backupState -> onBackupStateChanged(backupState) }
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
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_back_up_wallet_settings_safety), vector_all_settings_backup_options_icon) {
                _navigation.postValue(AllSettingsNavigation.ToBackupOnboardingFlow)
            },
            SettingsTitleDto(resourceManager.getString(all_settings_secondary_settings_label)),
            ButtonViewDto(resourceManager.getString(tari_about_title), vector_all_settings_about_icon) {
                _navigation.postValue(AllSettingsNavigation.ToAbout)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_report_a_bug), vector_all_settings_report_bug_icon) {
                _navigation.postValue(AllSettingsNavigation.ToBugReporting)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_visit_site), vector_all_settings_visit_tari_icon) {
                _openLink.postValue(resourceManager.getString(tari_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_contribute), vector_all_settings_contribute_to_tari_icon) {
                _openLink.postValue(resourceManager.getString(github_repo_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_user_agreement), vector_all_settings_user_agreement_icon) {
                _openLink.postValue(resourceManager.getString(user_agreement_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_privacy_policy), vector_all_settings_privacy_policy_icon) {
                _openLink.postValue(resourceManager.getString(privacy_policy_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_disclaimer), vector_all_settings_disclaimer_icon) {
                _openLink.postValue(resourceManager.getString(disclaimer_url))
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_explorer), vector_all_settings_block_explorer_icon) {
                _openLink.postValue(resourceManager.getString(explorer_url))
            },
            SettingsTitleDto(resourceManager.getString(all_settings_yat_settings_label)),
            ButtonViewDto(resourceManager.getString(all_settings_connect_yats), vector_all_settings_yat_icon, vector_open_in_browser) {
                _openYatOnboarding.postValue(Unit)
            },
            SettingsTitleDto(resourceManager.getString(all_settings_advanced_settings_label)),
            ButtonViewDto(resourceManager.getString(all_settings_select_theme), vector_all_settings_select_theme_icon) {
                _navigation.postValue(AllSettingsNavigation.ToThemeSelection)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_background_service), vector_all_settings_background_service_icon) {
                _navigation.postValue(AllSettingsNavigation.ToBackgroundService)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_bridge_configuration), vector_all_settings_bridge_configuration_icon) {
                _navigation.postValue(AllSettingsNavigation.ToTorBridges)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_select_network), vector_all_settings_select_network_icon) {
                _navigation.postValue(AllSettingsNavigation.ToNetworkSelection)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_select_base_node), vector_all_settings_select_base_node_icon) {
                _navigation.postValue(AllSettingsNavigation.ToBaseNodeSelection)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_delete_wallet), vector_all_settings_delete_button_icon, null, ButtonStyle.Warning) {
                _navigation.postValue(AllSettingsNavigation.ToDeleteWallet)
            },
            DividerViewHolderItem(),
            SettingsVersionViewHolderItem(versionText) { _copyToClipboard.postValue(versionArgs) }
        )
        _allSettingsOptions.value = allOptions
    }

    private fun onBackupStateChanged(backupState: BackupsState?) {
        if (backupState == null) {
            backupOption.backupState = PresentationBackupState(Warning)
        } else {
            val presentationBackupState = when (backupState.backupsState) {
                is BackupState.BackupDisabled -> PresentationBackupState(Warning)
                is BackupState.BackupInProgress -> {
                    PresentationBackupState(InProgress, back_up_wallet_backup_status_in_progress, R.attr.palette_text_body)
                }
                is BackupState.BackupUpToDate -> {
                    PresentationBackupState(Success, back_up_wallet_backup_status_up_to_date, R.attr.palette_system_green)
                }
                is BackupState.BackupFailed -> {
                    PresentationBackupState(Warning, back_up_wallet_backup_status_outdated, R.attr.palette_system_red)
                }
            }
            backupOption.backupState = presentationBackupState
        }
        _allSettingsOptions.postValue(_allSettingsOptions.value)
    }

    private fun showBackupStorageCheckFailedDialog(message: String) {
        val errorArgs = ErrorDialogArgs(resourceManager.getString(check_backup_storage_status_error_title), message)
        _modularDialog.postValue(errorArgs.getModular(resourceManager))
    }
}

