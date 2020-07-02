package com.tari.android.wallet.infrastructure.backup

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

internal object BackupNamingPolicy {

    private const val backupFileNamePrefix = "Tari-Aurora-Backup-"

    // Tari-Aurora-Backup_yyyy-MM-dd_HH-mm-ss.*
    private val regex =
        Regex("Tari-Aurora-Backup-(\\d{4}-(((0)[1-9])|((1)[0-2]))-((0)[1-9]|[1-2][0-9]|(3)[0-1])_([0-1][0-9]|(2)[0-3])-([0-5][0-9])-([0-5][0-9]))\\..+")
    val dateFormatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd_HH-mm-ss")

    fun getBackupFileName(backupDate: DateTime = DateTime.now()): String {
        return backupFileNamePrefix + dateFormatter.print(backupDate)
    }

    fun isBackupFileName(fileName: String): Boolean {
        return regex.matches(fileName)
    }

    fun getDateFromBackupFileName(name: String): DateTime? =
        regex.find(name)?.let { it.groups[1]!!.value }?.let { dateFormatter.parseDateTime(it) }

}