package com.tari.android.wallet.ui.fragment.settings.allSettings

interface AllSettingsRouter {
    fun toAbout()

    fun toBackupSettings(withAnimation: Boolean = true)

    fun toDeleteWallet()

    fun toBackgroundService()

    fun toNetworkSelection()

    fun toThemeSelection()

    fun toTorBridges()

    fun toCustomTorBridges()

    fun toBaseNodeSelection()
}