package com.tari.android.wallet.infrastructure.backup

import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption

data class BackupMapState(val states: Map<BackupOption, BackupState> = emptyMap()) {

    val backupsState: BackupState
        get() {
            val backupsStates = states.values.toList()
            return backupsStates.firstOrNull { it is BackupState.BackupFailed }
                ?: backupsStates.firstOrNull { it is BackupState.BackupUpToDate }
                ?: backupsStates.firstOrNull { it is BackupState.BackupInProgress }
                ?: BackupState.BackupDisabled
        }
}