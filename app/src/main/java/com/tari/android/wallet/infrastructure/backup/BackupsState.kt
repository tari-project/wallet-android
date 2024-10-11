package com.tari.android.wallet.infrastructure.backup

import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOption

data class BackupsState(val backupsStates: Map<BackupOption, BackupState>) {

    val backupsState: BackupState
        get() {
            val backupsStates = backupsStates.values.toList()
            return backupsStates.firstOrNull { it is BackupState.BackupFailed }
                ?: backupsStates.firstOrNull { it is BackupState.BackupUpToDate }
                ?: backupsStates.firstOrNull { it is BackupState.BackupInProgress }
                ?: BackupState.BackupDisabled
        }
}