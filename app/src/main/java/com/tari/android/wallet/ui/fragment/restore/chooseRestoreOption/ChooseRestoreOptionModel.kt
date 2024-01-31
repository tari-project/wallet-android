package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType

object ChooseRestoreOptionModel {

    data class UiState(
        val selectedOption: BackupOptionType? = null,
        val options: List<BackupOptionDto> = emptyList(),
    )

    sealed interface Effect {
        data class BeginProgress(val optionType: BackupOptionType) : Effect
        data class EndProgress(val optionType: BackupOptionType) : Effect
        data class SetupStorage(val backupManager: BackupManager, val optionType: BackupOptionType) : Effect
    }
}