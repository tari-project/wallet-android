package com.tari.android.wallet.infrastructure.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupStateHandler @Inject constructor() {

    private val _backupState = MutableStateFlow(BackupMapState())
    val backupState = _backupState.asStateFlow()

    fun updateBackupState(backupMapState: BackupMapState) {
        _backupState.update { backupMapState }
    }
}