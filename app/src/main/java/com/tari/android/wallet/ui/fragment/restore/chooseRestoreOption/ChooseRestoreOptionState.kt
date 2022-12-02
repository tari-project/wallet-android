package com.tari.android.wallet.ui.fragment.restore.chooseRestoreOption

import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions

sealed class ChooseRestoreOptionState(val backupOptions: BackupOptions) {
    class BeginProgress(backupOptions: BackupOptions) : ChooseRestoreOptionState(backupOptions)

    class EndProgress(backupOptions: BackupOptions) : ChooseRestoreOptionState(backupOptions)
}