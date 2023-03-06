package com.tari.android.wallet.ui.fragment.settings.allSettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.drawable.vector_all_settings_about_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_background_service_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_backup_options_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_block_explorer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_bridge_configuration_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_contribute_to_tari_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_delete_button_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_disclaimer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_privacy_policy_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_report_bug_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_base_node_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_network_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_theme_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_user_agreement_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_visit_tari_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_yat_icon
import com.tari.android.wallet.R.string.all_settings_advanced_settings_label
import com.tari.android.wallet.R.string.all_settings_background_service
import com.tari.android.wallet.R.string.all_settings_bridge_configuration
import com.tari.android.wallet.R.string.all_settings_connect_yats
import com.tari.android.wallet.R.string.all_settings_contribute
import com.tari.android.wallet.R.string.all_settings_delete_wallet
import com.tari.android.wallet.R.string.all_settings_disclaimer
import com.tari.android.wallet.R.string.all_settings_explorer
import com.tari.android.wallet.R.string.all_settings_privacy_policy
import com.tari.android.wallet.R.string.all_settings_report_a_bug
import com.tari.android.wallet.R.string.all_settings_secondary_settings_label
import com.tari.android.wallet.R.string.all_settings_security_label
import com.tari.android.wallet.R.string.all_settings_select_base_node
import com.tari.android.wallet.R.string.all_settings_select_network
import com.tari.android.wallet.R.string.all_settings_select_theme
import com.tari.android.wallet.R.string.all_settings_user_agreement
import com.tari.android.wallet.R.string.all_settings_version_text_copy_title
import com.tari.android.wallet.R.string.all_settings_version_text_copy_toast_message
import com.tari.android.wallet.R.string.all_settings_visit_site
import com.tari.android.wallet.R.string.back_up_wallet_backup_status_in_progress
import com.tari.android.wallet.R.string.back_up_wallet_backup_status_outdated
import com.tari.android.wallet.R.string.back_up_wallet_backup_status_up_to_date
import com.tari.android.wallet.R.string.check_backup_storage_status_error_title
import com.tari.android.wallet.R.string.disclaimer_url
import com.tari.android.wallet.R.string.explorer_url
import com.tari.android.wallet.R.string.github_repo_url
import com.tari.android.wallet.R.string.privacy_policy_url
import com.tari.android.wallet.R.string.tari_about_title
import com.tari.android.wallet.R.string.tari_url
import com.tari.android.wallet.R.string.user_agreement_url
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupsState
import com.tari.android.wallet.ui.common.ClipboardArgs
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.InProgress
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.Success
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.Warning
import com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions.SettingsBackupOptionViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewDto
import com.tari.android.wallet.ui.fragment.settings.allSettings.myProfile.MyProfileViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolderItem
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

    @Inject
    lateinit var settingsRepository: SharedPrefsRepository

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
            MyProfileViewHolderItem(settingsRepository.emojiId.orEmpty()) {
                _navigation.postValue(AllSettingsNavigation.ToMyProfile)
            },
            DividerViewHolderItem(),
            ButtonViewDto(resourceManager.getString(all_settings_connect_yats), vector_all_settings_yat_icon) {
                _openYatOnboarding.postValue(Unit)
            },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_security_label)),
            backupOption,
            DividerViewHolderItem(),
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_secondary_settings_label)),
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
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_advanced_settings_label)),
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

