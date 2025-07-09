package com.tari.android.wallet.ui.screen.settings.allSettings

import androidx.lifecycle.LiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.drawable.vector_all_settings_about_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_backup_options_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_block_explorer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_bridge_configuration_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_cart
import com.tari.android.wallet.R.drawable.vector_all_settings_contacts_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_contribute_to_tari_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_data_collection
import com.tari.android.wallet.R.drawable.vector_all_settings_delete_button_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_disclaimer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_passcode
import com.tari.android.wallet.R.drawable.vector_all_settings_privacy_policy_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_report_bug_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_screen_recording_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_network_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_select_theme_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_user_agreement_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_visit_tari_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_yat_icon
import com.tari.android.wallet.R.drawable.vector_fingerprint
import com.tari.android.wallet.R.string.all_settings_advanced_settings_label
import com.tari.android.wallet.R.string.all_settings_biometrics
import com.tari.android.wallet.R.string.all_settings_bridge_configuration
import com.tari.android.wallet.R.string.all_settings_connect_yats
import com.tari.android.wallet.R.string.all_settings_contact_label
import com.tari.android.wallet.R.string.all_settings_contacts
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
import com.tari.android.wallet.R.string.disclaimer_url
import com.tari.android.wallet.R.string.github_repo_url
import com.tari.android.wallet.R.string.privacy_policy_url
import com.tari.android.wallet.R.string.tari_about_title
import com.tari.android.wallet.R.string.tari_url
import com.tari.android.wallet.R.string.ttl_store_url
import com.tari.android.wallet.R.string.user_agreement_url
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.Navigation.AllSettings
import com.tari.android.wallet.application.YatAdapter
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.DividerViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.SpaceVerticalViewHolderItem
import com.tari.android.wallet.ui.screen.pinCode.PinCodeScreenBehavior
import com.tari.android.wallet.ui.screen.settings.allSettings.PresentationBackupState.BackupStateStatus.InProgress
import com.tari.android.wallet.ui.screen.settings.allSettings.PresentationBackupState.BackupStateStatus.Success
import com.tari.android.wallet.ui.screen.settings.allSettings.PresentationBackupState.BackupStateStatus.Warning
import com.tari.android.wallet.ui.screen.settings.allSettings.backupOptions.SettingsBackupOptionViewHolderItem
import com.tari.android.wallet.ui.screen.settings.allSettings.myProfile.MyProfileViewHolderItem
import com.tari.android.wallet.ui.screen.settings.allSettings.row.SettingsRowStyle
import com.tari.android.wallet.ui.screen.settings.allSettings.row.SettingsRowViewHolderItem
import com.tari.android.wallet.ui.screen.settings.allSettings.title.SettingsTitleViewHolderItem
import com.tari.android.wallet.ui.screen.settings.allSettings.version.SettingsVersionViewHolderItem
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.addTo
import com.tari.android.wallet.util.extension.collectFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AllSettingsViewModel : CommonViewModel() {

    private val backupOption = SettingsBackupOptionViewHolderItem(leftIconId = vector_all_settings_backup_options_icon) {
        runWithAuthorization { tariNavigator.navigate(AllSettings.ToBackupSettings(true)) }
    }

    @Inject
    lateinit var yatAdapter: YatAdapter

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

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
        collectFlow(backupStateHandler.backupState) { onBackupStateChanged(it) }

        settingsRepository.updateNotifier.subscribe(
            /* onNext = */ { generateOptions() },
            /* onError = */ { logger.d("Error updating settings options", it) },
        ).addTo(compositeDisposable)
    }

    fun updateOptions() {
        _allSettingsOptions.update { generateOptions() }
    }

    private fun generateOptions(): List<CommonViewHolderItem> {
        if (!settingsRepository.walletAddressExists()) return emptyList() // Return empty list if this method called after wallet is deleted

        val versionText = TariVersionModel(networkRepository).versionInfo

        val alias = settingsRepository.alias.orEmpty()
        val pinCode = securityPrefRepository.pinCode

        return listOfNotNull(
            MyProfileViewHolderItem(
                address = settingsRepository.walletAddress,
                yat = yatAdapter.connectedYat.orEmpty(),
                alias = alias,
                action = { tariNavigator.navigate(AllSettings.ToMyProfile) },
            ),
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_connect_yats), vector_all_settings_yat_icon) {
                _openYatOnboarding.postValue(Unit)
            }.takeIf { DebugConfig.isYatEnabled },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_contact_label)),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_contacts), vector_all_settings_contacts_icon) {
                tariNavigator.navigate(Navigation.ContactBook.AllContacts())
            },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_security_label)),
            backupOption,
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_data_collection), vector_all_settings_data_collection) {
                tariNavigator.navigate(AllSettings.ToDataCollection)
            },
            DividerViewHolderItem(),
            if (pinCode != null) {
                SettingsRowViewHolderItem(resourceManager.getString(all_settings_pin_code), vector_all_settings_passcode) {
                    runWithAuthorization {
                        tariNavigator.navigate(Navigation.EnterPinCode(PinCodeScreenBehavior.ChangeNew))
                    }
                }
            } else {
                SettingsRowViewHolderItem(resourceManager.getString(all_settings_create_pin_code), vector_all_settings_passcode) {
                    runWithAuthorization {
                        tariNavigator.navigate(Navigation.EnterPinCode(PinCodeScreenBehavior.Create))
                    }
                }
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_biometrics), vector_fingerprint) {
                runWithAuthorization {
                    tariNavigator.navigate(Navigation.ChangeBiometrics)
                }
            },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_secondary_settings_label)),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_store), vector_all_settings_cart) {
                openUrl(resourceManager.getString(ttl_store_url))
            }.takeIf { DebugConfig.showTtlStoreMenu },
            DividerViewHolderItem().takeIf { DebugConfig.showTtlStoreMenu },
            SettingsRowViewHolderItem(resourceManager.getString(tari_about_title), vector_all_settings_about_icon) {
                tariNavigator.navigate(AllSettings.ToAbout)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_report_a_bug), vector_all_settings_report_bug_icon) {
                tariNavigator.navigate(AllSettings.ToBugReporting)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_visit_site), vector_all_settings_visit_tari_icon) {
                openUrl(resourceManager.getString(tari_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_contribute), vector_all_settings_contribute_to_tari_icon) {
                openUrl(resourceManager.getString(github_repo_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_user_agreement), vector_all_settings_user_agreement_icon) {
                openUrl(resourceManager.getString(user_agreement_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_privacy_policy), vector_all_settings_privacy_policy_icon) {
                openUrl(resourceManager.getString(privacy_policy_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_disclaimer), vector_all_settings_disclaimer_icon) {
                openUrl(resourceManager.getString(disclaimer_url))
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_explorer), vector_all_settings_block_explorer_icon) {
                openUrl(networkRepository.currentNetwork.blockExplorerBaseUrl.orEmpty())
            }.takeIf { networkRepository.currentNetwork.isBlockExplorerAvailable },
            SettingsTitleViewHolderItem(resourceManager.getString(all_settings_advanced_settings_label)),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_select_theme), vector_all_settings_select_theme_icon) {
                tariNavigator.navigate(AllSettings.ToThemeSelection)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(
                title = resourceManager.getString(all_settings_screen_recording),
                leftIconId = vector_all_settings_screen_recording_icon,
                warning = tariSettingsSharedRepository.screenRecordingTurnedOn,
            ) {
                tariNavigator.navigate(AllSettings.ToScreenRecording)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_bridge_configuration), vector_all_settings_bridge_configuration_icon) {
                tariNavigator.navigate(AllSettings.ToTorBridges)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_select_network), vector_all_settings_select_network_icon) {
                tariNavigator.navigate(AllSettings.ToNetworkSelection)
            },
            DividerViewHolderItem(),
            SettingsRowViewHolderItem(
                title = resourceManager.getString(all_settings_delete_wallet),
                leftIconId = vector_all_settings_delete_button_icon,
                iconId = null,
                style = SettingsRowStyle.Warning,
            ) { tariNavigator.navigate(AllSettings.ToDeleteWallet) },
            DividerViewHolderItem(),
            SettingsVersionViewHolderItem(versionText) {
                copyToClipboard(
                    clipLabel = resourceManager.getString(all_settings_version_text_copy_title),
                    clipText = versionText,
                    toastMessage = resourceManager.getString(all_settings_version_text_copy_toast_message),
                )
            },
            SpaceVerticalViewHolderItem(48),
        )
    }

    private fun onBackupStateChanged(backupState: BackupState) {
        backupOption.backupState = when (backupState) {
            is BackupState.BackupDisabled -> PresentationBackupState(Warning)
            is BackupState.BackupInProgress -> PresentationBackupState(InProgress, back_up_wallet_backup_status_in_progress, R.attr.palette_text_body)
            is BackupState.BackupUpToDate -> PresentationBackupState(Success, back_up_wallet_backup_status_up_to_date, R.attr.palette_system_green)
            is BackupState.BackupFailed -> PresentationBackupState(Warning, back_up_wallet_backup_status_outdated, R.attr.palette_system_red)
        }
        _allSettingsOptions.update { generateOptions() }
    }
}
