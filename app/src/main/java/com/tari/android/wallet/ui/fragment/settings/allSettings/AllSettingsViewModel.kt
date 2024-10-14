package com.tari.android.wallet.ui.fragment.settings.allSettings

import androidx.lifecycle.LiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.drawable.vector_all_settings_about_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_background_service_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_backup_options_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_block_explorer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_bluetooth
import com.tari.android.wallet.R.drawable.vector_all_settings_bridge_configuration_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_cart
import com.tari.android.wallet.R.drawable.vector_all_settings_contribute_to_tari_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_data_collection
import com.tari.android.wallet.R.drawable.vector_all_settings_delete_button_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_disclaimer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_passcode
import com.tari.android.wallet.R.drawable.vector_all_settings_privacy_policy_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_report_bug_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_screen_recording_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_base_node_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_network_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_theme_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_user_agreement_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_visit_tari_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_yat_icon
import com.tari.android.wallet.R.drawable.vector_fingerprint
import com.tari.android.wallet.R.string.all_settings_advanced_settings_label
import com.tari.android.wallet.R.string.all_settings_background_service
import com.tari.android.wallet.R.string.all_settings_biometrics
import com.tari.android.wallet.R.string.all_settings_bluetooth_settings
import com.tari.android.wallet.R.string.all_settings_bridge_configuration
import com.tari.android.wallet.R.string.all_settings_connect_yats
import com.tari.android.wallet.R.string.all_settings_contribute
import com.tari.android.wallet.R.string.all_settings_create_pin_code
import com.tari.android.wallet.R.string.all_settings_data_collection
import com.tari.android.wallet.R.string.all_settings_delete_wallet
import com.tari.android.wallet.R.string.all_settings_disclaimer
import com.tari.android.wallet.R.string.all_settings_explorer
import com.tari.android.wallet.R.string.all_settings_pin_code
import com.tari.android.wallet.R.string.all_settings_privacy_policy
import com.tari.android.wallet.R.string.all_settings_report_a_bug
import com.tari.android.wallet.R.string.all_settings_screen_recording
import com.tari.android.wallet.R.string.all_settings_secondary_settings_label
import com.tari.android.wallet.R.string.all_settings_security_label
import com.tari.android.wallet.R.string.all_settings_select_base_node
import com.tari.android.wallet.R.string.all_settings_select_network
import com.tari.android.wallet.R.string.all_settings_select_theme
import com.tari.android.wallet.R.string.all_settings_store
import com.tari.android.wallet.R.string.all_settings_user_agreement
import com.tari.android.wallet.R.string.all_settings_version_text_copy_title
import com.tari.android.wallet.R.string.all_settings_version_text_copy_toast_message
import com.tari.android.wallet.R.string.all_settings_visit_site
import com.tari.android.wallet.R.string.back_up_wallet_backup_status_in_progress
import com.tari.android.wallet.R.string.back_up_wallet_backup_status_outdated
import com.tari.android.wallet.R.string.back_up_wallet_backup_status_up_to_date
import com.tari.android.wallet.R.string.check_backup_storage_status_error_title
import com.tari.android.wallet.R.string.disclaimer_url
import com.tari.android.wallet.R.string.github_repo_url
import com.tari.android.wallet.R.string.privacy_policy_url
import com.tari.android.wallet.R.string.tari_about_title
import com.tari.android.wallet.R.string.tari_url
import com.tari.android.wallet.R.string.ttl_store_url
import com.tari.android.wallet.R.string.user_agreement_url
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.addTo
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupsState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation.AllSettingsNavigation
import com.tari.android.wallet.ui.fragment.pinCode.PinCodeScreenBehavior
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.InProgress
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.Success
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState.BackupStateStatus.Warning
import com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions.SettingsBackupOptionViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.myProfile.MyProfileViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.row.SettingsRowStyle
import com.tari.android.wallet.ui.fragment.settings.allSettings.row.SettingsRowViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.version.SettingsVersionViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.userAutorization.BiometricAuthenticationViewModel
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AllSettingsViewModel : CommonViewModel() {

    lateinit var authenticationViewModel: BiometricAuthenticationViewModel

    private val backupOption = SettingsBackupOptionViewHolderItem(leftIconId = vector_all_settings_backup_options_icon) {
        runWithAuthorization { navigation.postValue(AllSettingsNavigation.ToBackupSettings) }
    }

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var settingsRepository: CorePrefRepository

    init {
        component.inject(this)
    }

    private val _openYatOnboarding = SingleLiveEvent<Unit>()
    val openYatOnboarding: LiveData<Unit> = _openYatOnboarding

    private val _allSettingsOptions = MutableStateFlow(generateOptions())
    val allSettingsOptions = _allSettingsOptions.asStateFlow()

    init {
        EventBus.backupState.subscribe(this) { backupState -> onBackupStateChanged(backupState) }

        settingsRepository.updateNotifier.subscribe { generateOptions() }.addTo(compositeDisposable)
    }

    fun updateOptions() {
        _allSettingsOptions.update { generateOptions() }
    }

    private fun generateOptions(): List<CommonViewHolderItem> {
        if (!settingsRepository.walletAddressExists()) return emptyList() // Return empty list if this method called after wallet is deleted

        val versionText = TariVersionModel(networkRepository).versionInfo

        val alias = settingsRepository.firstName.orEmpty() + " " + settingsRepository.lastName.orEmpty()
        val pinCode = securityPrefRepository.pinCode

        return listOfNotNull(
            MyProfileViewHolderItem(
                address = settingsRepository.walletAddress,
                yat = yatAdapter.connectedYat.orEmpty(),
                alias = alias,
                action = { tariNavigator.navigate(AllSettingsNavigation.ToMyProfile) },
            ),
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_connect_yats), vector_all_settings_yat_icon) {
                _openYatOnboarding.postValue(Unit)
            }.takeIf { DebugConfig.isYatEnabled },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_security_label)),
            backupOption,
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_data_collection), vector_all_settings_data_collection) {
                navigation.postValue(AllSettingsNavigation.ToDataCollection)
            },
            DividerViewHolderItem(),
            if (pinCode != null) {
                SettingsRowViewHolderItem(resourceManager.getString(all_settings_pin_code), vector_all_settings_passcode) {
                    runWithAuthorization {
                        navigation.postValue(Navigation.EnterPinCodeNavigation(PinCodeScreenBehavior.ChangeNew))
                    }
                }
            } else {
                SettingsRowViewHolderItem(resourceManager.getString(all_settings_create_pin_code), vector_all_settings_passcode) {
                    runWithAuthorization {
                        navigation.postValue(Navigation.EnterPinCodeNavigation(PinCodeScreenBehavior.Create))
                    }
                }
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_biometrics), vector_fingerprint) {
                runWithAuthorization {
                    navigation.postValue(Navigation.ChangeBiometrics)
                }
            },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_secondary_settings_label)),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_store), vector_all_settings_cart) {
                _openLink.postValue(resourceManager.getString(ttl_store_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(tari_about_title), vector_all_settings_about_icon) {
                navigation.postValue(AllSettingsNavigation.ToAbout)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_report_a_bug), vector_all_settings_report_bug_icon) {
                navigation.postValue(AllSettingsNavigation.ToBugReporting)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_visit_site), vector_all_settings_visit_tari_icon) {
                _openLink.postValue(resourceManager.getString(tari_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_contribute), vector_all_settings_contribute_to_tari_icon) {
                _openLink.postValue(resourceManager.getString(github_repo_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_user_agreement), vector_all_settings_user_agreement_icon) {
                _openLink.postValue(resourceManager.getString(user_agreement_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_privacy_policy), vector_all_settings_privacy_policy_icon) {
                _openLink.postValue(resourceManager.getString(privacy_policy_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_disclaimer), vector_all_settings_disclaimer_icon) {
                _openLink.postValue(resourceManager.getString(disclaimer_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_explorer), vector_all_settings_block_explorer_icon) {
                _openLink.postValue(networkRepository.currentNetwork.blockExplorerUrl.orEmpty())
            }.takeIf { networkRepository.currentNetwork.isBlockExplorerAvailable },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_advanced_settings_label)),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_select_theme), vector_all_settings_select_theme_icon) {
                navigation.postValue(AllSettingsNavigation.ToThemeSelection)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_background_service), vector_all_settings_background_service_icon) {
                navigation.postValue(AllSettingsNavigation.ToBackgroundService)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(
                title = resourceManager.getString(all_settings_screen_recording),
                leftIconId = vector_all_settings_screen_recording_icon,
                warning = tariSettingsSharedRepository.screenRecordingTurnedOn,
            ) {
                navigation.postValue(AllSettingsNavigation.ToScreenRecording)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_bluetooth_settings), vector_all_settings_bluetooth) {
                navigation.postValue(AllSettingsNavigation.ToBluetoothSettings)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_bridge_configuration), vector_all_settings_bridge_configuration_icon) {
                navigation.postValue(AllSettingsNavigation.ToTorBridges)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_select_network), vector_all_settings_select_network_icon) {
                navigation.postValue(AllSettingsNavigation.ToNetworkSelection)
            },
            DividerViewHolderItem().takeIf { DebugConfig.selectBaseNodeEnabled },
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_select_base_node), vector_all_settings_select_base_node_icon) {
                navigation.postValue(AllSettingsNavigation.ToBaseNodeSelection)
            }.takeIf { DebugConfig.selectBaseNodeEnabled },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(
                title = resourceManager.getString(all_settings_delete_wallet),
                leftIconId = vector_all_settings_delete_button_icon,
                iconId = null,
                style = SettingsRowStyle.Warning,
            ) { navigation.postValue(AllSettingsNavigation.ToDeleteWallet) },
            DividerViewHolderItem(),
            SettingsVersionViewHolderItem(versionText) {
                copyToClipboard(
                    clipLabel = resourceManager.getString(all_settings_version_text_copy_title),
                    clipText = versionText,
                    toastMessage = resourceManager.getString(all_settings_version_text_copy_toast_message),
                )
            })
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
        _allSettingsOptions.update { generateOptions() }
    }

    private fun showBackupStorageCheckFailedDialog(message: String) {
        showSimpleDialog(
            title = resourceManager.getString(check_backup_storage_status_error_title),
            description = message,
        )
    }
}
