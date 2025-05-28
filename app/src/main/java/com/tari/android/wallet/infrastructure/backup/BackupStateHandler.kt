package com.tari.android.wallet.infrastructure.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupStateHandler @Inject constructor() {

    private val _backupState = MutableStateFlow<BackupState>(BackupState.BackupDisabled)
    val backupState = _backupState.asStateFlow()

    val inProgress: Boolean
        get() = _backupState.value is BackupState.BackupInProgress

    fun updateBackupState(backupState: BackupState) {
        _backupState.update { backupState }
    }
}