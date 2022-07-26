package com.tari.android.wallet.ui.fragment.settings.allSettings

class PresentationBackupState(val status: BackupStateStatus, val textId: Int = -1, val textColor: Int = -1) {
    enum class BackupStateStatus {
        InProgress,
        Success,
        Warning,
        Scheduled
    }
}