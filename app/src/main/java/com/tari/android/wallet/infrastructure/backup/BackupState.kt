package com.tari.android.wallet.infrastructure.backup

import java.lang.Exception

/**
 * Backup status.
 *
 * @author The Tari Development Team
 */
internal sealed class BackupState
internal object BackupDisabled : BackupState()
internal object BackupCheckingStorage : BackupState()
internal object BackupStorageCheckFailed : BackupState()
internal object BackupScheduled : BackupState()
internal object BackupInProgress : BackupState()
internal object BackupUpToDate : BackupState()
internal data class BackupOutOfDate(val backupException: Exception? = null) : BackupState()
