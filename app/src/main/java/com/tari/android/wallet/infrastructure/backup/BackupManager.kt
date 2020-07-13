package com.tari.android.wallet.infrastructure.backup

import com.orhanobut.logger.Logger
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.SharedPrefsWrapper
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.net.ConnectException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.math.max

internal class BackupManager(
    private val sharedPrefs: SharedPrefsWrapper,
    private val backupStorage: BackupStorage
) {

    private var retryCount = 0

    /**
     * Timer to trigger scheduled backups.
     */
    private var scheduledBackupSubscription: Disposable? = null

    fun start() {
        Logger.d("Start backup manager.")
        val scheduledBackupDate = sharedPrefs.scheduledBackupDate
        when {
            !sharedPrefs.backupIsEnabled -> {
                EventBus.postBackupState(BackupDisabled)
            }
            sharedPrefs.backupFailureDate != null -> {
                EventBus.postBackupState(BackupFailed())
            }
            scheduledBackupDate != null -> {
                val delayMs = scheduledBackupDate.millis - DateTime.now().millis
                scheduleBackup(
                    delayMs = max(0, delayMs),
                    resetRetryCount = true
                )
            }
            else -> {
                EventBus.postBackupState(BackupUpToDate)
            }
        }
    }

    suspend fun checkStorageStatus() {
        if (!sharedPrefs.backupIsEnabled) {
            return
        }
        if (EventBus.backupStateSubject.value is BackupInProgress) {
            return
        }
        Logger.d("Check backup storage status.")
        EventBus.postBackupState(BackupCheckingStorage)
        // check storage
        try {
            val backupDate = sharedPrefs.lastSuccessfulBackupDate!!
            if (!backupStorage.hasBackupForDate(backupDate)) {
                throw BackupStorageTamperedException("Backup storage is tampered.")
            }
            if (sharedPrefs.scheduledBackupDate?.isAfterNow == true) {
                EventBus.postBackupState(BackupScheduled)
            } else {
                EventBus.postBackupState(BackupUpToDate)
            }
        } catch (exception: Exception) {
            Logger.e(exception, "Error while checking storage. %s", exception.toString())
            sharedPrefs.backupFailureDate = DateTime.now()
            if (sharedPrefs.scheduledBackupDate?.isAfterNow == true) {
                EventBus.postBackupState(BackupScheduled)
            } else {
                EventBus.postBackupState(BackupFailed(exception))
            }
            throw exception
        }
    }

    fun scheduleBackup(
        delayMs: Long? = null,
        resetRetryCount: Boolean = false
    ) {
        if (!sharedPrefs.backupIsEnabled) {
            Logger.i("Backup is not enabled - cannot schedule a backup.")
            return // if backup is disabled or there's a scheduled backup
        }
        if (scheduledBackupSubscription?.isDisposed == false) {
            Logger.i("There is already a scheduled backup - cannot schedule a backup.")
            return // if backup is disabled or there's a scheduled backup
        }
        Logger.d("Schedule backup.")
        if (resetRetryCount) {
            retryCount = 0
        }
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription =
            Observable
                .timer(delayMs ?: Constants.Wallet.backupDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    GlobalScope.launch(Dispatchers.IO) {
                        backup()
                    }
                }
        sharedPrefs.scheduledBackupDate =
            DateTime.now().plusMillis(delayMs?.toInt() ?: Constants.Wallet.backupDelayMs.toInt())
        EventBus.postBackupState(BackupScheduled)
        Logger.i("Backup scheduled.")
    }

    suspend fun backup(
        isInitialBackup: Boolean = false,
        newPassword: CharArray? = null
    ) {
        if (!isInitialBackup && !sharedPrefs.backupIsEnabled) {
            Logger.d("Backup is disabled. Exit.")
            return
        }
        if (EventBus.backupStateSubject.value is BackupInProgress) {
            Logger.d("Backup is in progress. Exit.")
            return
        }
        // Do the work here--in this case, upload the images.
        Logger.d("Do backup.")
        // cancel any scheduled backup
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription = null
        retryCount++
        EventBus.postBackupState(BackupInProgress)
        try {
            Logger.e("MANG PASS: %s", newPassword?.joinToString(separator = ""))
            val backupDate = backupStorage.backup(newPassword)
            sharedPrefs.lastSuccessfulBackupDate = backupDate
            sharedPrefs.scheduledBackupDate = null
            sharedPrefs.backupFailureDate = null
            Logger.d("Backup successful.")
            EventBus.postBackupState(BackupUpToDate)
        } catch (exception: Exception) {
            if (isInitialBackup || retryCount > Constants.Wallet.maxBackupRetries) {
                Logger.e(
                    exception,
                    "Error happened while backing up. It's an initial backup or retry limit has been exceeded."
                )
                EventBus.postBackupState(BackupFailed(exception))
                sharedPrefs.scheduledBackupDate = null
                sharedPrefs.backupFailureDate = DateTime.now()
                return
            } else {
                Logger.e(exception, "Backup failed: ${exception.message}. Will schedule.")
                scheduleBackup(
                    delayMs = Constants.Wallet.backupRetryPeriodMs,
                    resetRetryCount = false
                )
            }
            Logger.e(exception, "Error happened while backing up. Will retry.")
        }
    }

    suspend fun clear() {
        backupStorage.deleteAllBackupFiles()
        sharedPrefs.lastSuccessfulBackupDate = null
        sharedPrefs.backupFailureDate = null
        sharedPrefs.backupPassword = null
        sharedPrefs.scheduledBackupDate = null
        sharedPrefs.localBackupFolderURI = null
        EventBus.postBackupState(BackupDisabled)
    }

}