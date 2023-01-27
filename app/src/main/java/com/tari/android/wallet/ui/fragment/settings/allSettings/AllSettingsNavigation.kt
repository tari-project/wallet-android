package com.tari.android.wallet.ui.fragment.settings.allSettings

sealed class AllSettingsNavigation {
    object ToBugReporting : AllSettingsNavigation()
    object ToAbout : AllSettingsNavigation()
    object ToBackupSettings : AllSettingsNavigation()
    object ToBackupOnboardingFlow : AllSettingsNavigation()
    object ToDeleteWallet : AllSettingsNavigation()
    object ToBackgroundService : AllSettingsNavigation()
    object ToThemeSelection : AllSettingsNavigation()
    object ToTorBridges : AllSettingsNavigation()
    object ToNetworkSelection : AllSettingsNavigation()
    object ToBaseNodeSelection : AllSettingsNavigation()
}