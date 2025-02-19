package com.tari.android.wallet.ui.screen.settings.backup.learnMore.item

object BackupLearnMoreDataSource {

    private val dataSource = mutableListOf<BackupLearnMoreStageArgs>()

    fun getByPosition(position: Int): BackupLearnMoreStageArgs = dataSource[position]

    fun save(list: List<BackupLearnMoreStageArgs>) {
        dataSource.clear()
        dataSource.addAll(list)
    }
}