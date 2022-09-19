package com.tari.android.wallet.ui.fragment.settings.allSettings

sealed class AllSettingsNavigation {
    object ToAbout : AllSettingsNavigation()
    object ToBackupSettings : AllSettingsNavigation()
    object ToDeleteWallet : AllSettingsNavigation()
    object ToBackgroundService : AllSettingsNavigation()
    object ToTorBridges : AllSettingsNavigation()
    object ToNetworkSelection : AllSettingsNavigation()
    object ToBaseNodeSelection : AllSettingsNavigation()
}