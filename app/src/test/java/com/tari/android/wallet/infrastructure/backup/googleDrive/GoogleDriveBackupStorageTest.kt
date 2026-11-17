package com.tari.android.wallet.infrastructure.backup.googleDrive

import org.junit.Assert.assertEquals
import org.junit.Test

class GoogleDriveBackupStorageTest {

    private val isBackupFileName: (String) -> Boolean = { Regex("Tari-Aurora-Backup-mainnet.*").matches(it) }

    @Test
    fun `earlier backups of both kinds are superseded by the new one`() {
        val files = listOf(
            "new" to "Tari-Aurora-Backup-mainnet",
            "old-full" to "Tari-Aurora-Backup-mainnet",
            "old-partial" to "Tari-Aurora-Backup-mainnet.json",
        )

        val superseded = GoogleDriveBackupStorage.supersededBackupFileIds(files, keepFileId = "new", isBackupFileName)

        assertEquals(listOf("old-full", "old-partial"), superseded)
    }

    @Test
    fun `the new backup itself is never superseded`() {
        val files = listOf("new" to "Tari-Aurora-Backup-mainnet.json")

        assertEquals(emptyList<String>(), GoogleDriveBackupStorage.supersededBackupFileIds(files, "new", isBackupFileName))
    }

    @Test
    fun `files that are not backups of this network stay`() {
        val files: List<Pair<String?, String?>> = listOf(
            "new" to "Tari-Aurora-Backup-mainnet",
            "other-network" to "Tari-Aurora-Backup-esmeralda",
            "unrelated" to "settings.json",
            "no-name" to null,
            null to "Tari-Aurora-Backup-mainnet",
            "" to "Tari-Aurora-Backup-mainnet",
        )

        assertEquals(emptyList<String>(), GoogleDriveBackupStorage.supersededBackupFileIds(files, "new", isBackupFileName))
    }
}
