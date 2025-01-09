package com.tari.android.wallet.ui.screen.settings.backup.backupOnboarding.item

object BackupOnboardingFlowDataSource {

    private val dataSource = mutableListOf<BackupOnboardingArgs>()

    fun getByPosition(position: Int): BackupOnboardingArgs = dataSource[position]

    fun save(list: List<BackupOnboardingArgs>) {
        dataSource.clear()
        dataSource.addAll(list)
    }
}