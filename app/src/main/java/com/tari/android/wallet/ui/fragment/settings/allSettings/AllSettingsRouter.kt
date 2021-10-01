package com.tari.android.wallet.ui.fragment.settings.allSettings

interface AllSettingsRouter {
    fun toBackupSettings()

    fun toDeleteWallet()

    fun toBackgroundService()

    fun toNetworkSelection()

    fun toBaseNodeSelection()
}