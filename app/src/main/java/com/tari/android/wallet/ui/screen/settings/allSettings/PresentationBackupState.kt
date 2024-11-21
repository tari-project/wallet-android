package com.tari.android.wallet.ui.screen.settings.allSettings

class PresentationBackupState(val status: BackupStateStatus, val textId: Int = -1, val textColor: Int = -1) {
    enum class BackupStateStatus {
        InProgress,
        Success,
        Warning,
    }
}