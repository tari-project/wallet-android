package com.tari.android.wallet.infrastructure.backup

import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.data.sharedPrefs.security.SecurityPrefRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.GeneralSecurityException
import kotlin.random.Random

class BackupFileProcessorTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val databaseContent = Random(2).nextBytes(50_000)
    private val securityPrefs = mockk<SecurityPrefRepository>(relaxed = true)

    private fun processor(walletDir: File, password: String): BackupFileProcessor {
        val tempDir = File(walletDir, "temp").apply { mkdirs() }
        val walletConfig = mockk<WalletConfig> {
            every { getWalletFilesDirPath(any()) } returns walletDir.absolutePath
            every { getWalletTempDirPath() } returns tempDir.absolutePath
            every { getWalletDatabaseFilePath(any()) } returns File(walletDir, "tari_wallet.sqlite3").absolutePath
        }
        val backupPrefs = mockk<BackupPrefRepository>(relaxed = true) { every { backupPassword } returns password }
        val namingPolicy = mockk<BackupNamingPolicy> {
            every { getBackupFileName(any()) } returns "Tari-Aurora-Backup-mainnet"
            every { getPassphraseFileName() } returns "passphrase"
        }
        every { securityPrefs.databasePassphrase } returns "database passphrase"
        return BackupFileProcessor(backupPrefs, securityPrefs, walletConfig, namingPolicy, mockk(relaxed = true))
    }

    private fun createBackup(): File {
        val sourceDir = folder.newFolder("source")
        File(sourceDir, "tari_wallet.sqlite3").writeBytes(databaseContent)
        val (backupFile, _, _) = runBlocking { processor(sourceDir, "correct horse").generateBackupFile() }
        return backupFile.copyTo(File(folder.newFolder("cloud"), backupFile.name))
    }

    @Test
    fun `a full backup restores the database and the database passphrase`() {
        val backupFile = createBackup()
        val targetDir = folder.newFolder("target")

        processor(targetDir, "").restoreBackupFile(backupFile, "correct horse")

        assertEquals("TARIBK02", String(backupFile.readBytes().copyOfRange(0, 8), Charsets.US_ASCII))
        assertArrayEquals(databaseContent, File(targetDir, "tari_wallet.sqlite3").readBytes())
        verify { securityPrefs.databasePassphrase = "database passphrase" }
    }

    @Test
    fun `a full backup is not restored with a wrong password`() {
        val backupFile = createBackup()
        val targetDir = folder.newFolder("target")

        assertThrows(GeneralSecurityException::class.java) { processor(targetDir, "").restoreBackupFile(backupFile, "wrong horse") }
        assertEquals(false, File(targetDir, "tari_wallet.sqlite3").exists())
    }
}
