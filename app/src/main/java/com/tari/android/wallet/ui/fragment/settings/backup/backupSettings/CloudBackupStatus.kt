package com.tari.android.wallet.ui.fragment.settings.backup.backupSettings

import com.tari.android.wallet.R

sealed class CloudBackupStatus(val text: Int, val color: Int) {
    object Success : CloudBackupStatus(R.string.back_up_wallet_backup_status_up_to_date, R.color.all_settings_back_up_status_up_to_date)

    class InProgress(text: Int) : CloudBackupStatus(text, R.color.all_settings_back_up_status_processing)

    class Warning(text: Int = -1, color: Int = -1) : CloudBackupStatus(text, color)
}