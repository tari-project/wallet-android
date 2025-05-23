package com.tari.android.wallet.ui.screen.restore.chooseRestoreOption

import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOptionDto

object ChooseRestoreOptionModel {
    data class UiState(
        val backupOptions: List<BackupOptionDto> = emptyList(),

        val selectedOption: BackupOption? = null,
        val isStarted: Boolean = false,

        val paperWalletProgress: Boolean = false,
    )
}