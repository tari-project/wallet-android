package com.tari.android.wallet.infrastructure.backup

import com.tari.android.wallet.application.AppStateHandler
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.googleDrive.GoogleDriveBackupStorage
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.emptyFlow
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class BackupManagerTest {

    private val storage = mockk<GoogleDriveBackupStorage>(relaxed = true)
    private val backupPrefs = mockk<BackupPrefRepository>(relaxed = true)
    private val walletManager = mockk<WalletManager>(relaxed = true)
    private val appStateHandler = mockk<AppStateHandler>(relaxed = true)
    private val backupStateHandler = BackupStateHandler()

    private var option = BackupOption(isEnabled = true, lastSuccessDate = null, lastFailureDate = null)

    private fun createManager(): BackupManager {
        every { backupPrefs.currentBackupOption } answers { option }
        every { backupPrefs.updateOption(any()) } answers { option = firstArg() }
        every { walletManager.walletEvent } returns emptyFlow()
        every { appStateHandler.appEvent } returns emptyFlow()

        return BackupManager(
            context = mockk(relaxed = true),
            backupPrefs = backupPrefs,
            googleDriveBackupStorage = storage,
            notificationHelper = mockk(relaxed = true),
            walletManager = walletManager,
            appStateHandler = appStateHandler,
            backupStateHandler = backupStateHandler,
            applicationScope = CoroutineScope(Dispatchers.Unconfined),
        )
    }

    @Test
    fun `backupNow uploads a backup and reports success when the option is enabled`() {
        coEvery { storage.backup() } returns DateTime(1_700_000_000_000L)
        val manager = createManager()
        var succeeded = false
        var failed = false

        manager.backupNow(onSuccess = { succeeded = true }, onFailure = { failed = true })

        // once from the constructor, once from the explicit call
        coVerify(exactly = 2) { storage.backup() }
        assertTrue(succeeded)
        assertFalse(failed)
        assertEquals(BackupState.BackupUpToDate, backupStateHandler.backupState.value)
        assertEquals(1_700_000_000_000L, option.lastSuccessDate?.timeInLong)
        assertNull(option.lastFailureDate)
    }

    @Test
    fun `backupNow does not upload when the option is disabled`() {
        option = option.copy(isEnabled = false)
        val manager = createManager()
        var succeeded = false
        var failed = false

        manager.backupNow(onSuccess = { succeeded = true }, onFailure = { failed = true })

        coVerify(exactly = 0) { storage.backup() }
        assertFalse(succeeded)
        assertFalse(failed)
    }

    @Test
    fun `backupNow reports a failed upload and records the failure`() {
        val error = IOException("upload failed")
        coEvery { storage.backup() } throws error
        val manager = createManager()
        var reported: Throwable? = null

        manager.backupNow(onFailure = { reported = it })

        assertEquals(error, reported)
        assertTrue(backupStateHandler.backupState.value is BackupState.BackupFailed)
        assertNull(option.lastSuccessDate)
        assertNotNull(option.lastFailureDate)
    }
}
