package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings.option

import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType

object BackupOptionModel {
    data class UiState(
        val option: BackupOptionDto,
        val switchChecked: Boolean,
        val loading: Boolean = false,
        val lastSuccessDate: String? = null,
    ) {
        constructor(option: BackupOptionDto) : this(
            option = option,
            switchChecked = option.isEnabled,
        )

        fun startLoading() = this.copy(loading = true)
        fun stopLoading() = this.copy(loading = false)
        fun switchOn() = this.copy(switchChecked = true)
        fun switchOff() = this.copy(switchChecked = false)
        fun switch(value: Boolean) = this.copy(switchChecked = value)
    }


    sealed interface Effect {
        data class SetupStorage(val backupManager: BackupManager, val optionType: BackupOptionType) : Effect
    }
}