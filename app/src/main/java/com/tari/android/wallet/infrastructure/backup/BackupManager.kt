/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.infrastructure.backup

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.ui.fragment.settings.backup.BackupSettingsRepository
import com.tari.android.wallet.util.Constants
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joda.time.DateTime
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.max

class BackupManager(
    private val context: Context,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val backupStorage: BackupStorage,
    private val notificationHelper: NotificationHelper
) {

    private var retryCount = 0
    private val logger = Logger.t(BackupManager::class.simpleName)

    /**
     * Timer to trigger scheduled backups.
     */
    private var scheduledBackupSubscription: Disposable? = null

    fun initialize() {
        val scheduledBackupDate = backupSettingsRepository.scheduledBackupDate
        when {
            !backupSettingsRepository.backupIsEnabled -> EventBus.backupState.post(BackupState.BackupDisabled)
            backupSettingsRepository.backupFailureDate != null -> EventBus.backupState.post(BackupState.BackupOutOfDate())
            scheduledBackupDate != null -> scheduleBackup(max(0, scheduledBackupDate.millis - DateTime.now().millis), true)
            else -> EventBus.backupState.post(BackupState.BackupUpToDate)
        }
    }

    fun setupStorage(hostFragment: Fragment) {
        logger.i("Setup storage")
        backupStorage.setup(hostFragment)
    }

    suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        logger.i("Setup storage stage 2")
        backupStorage.onSetupActivityResult(requestCode, resultCode, intent)
    }

    suspend fun checkStorageStatus() {
        if (!backupSettingsRepository.backupIsEnabled) {
            return
        }
        if (EventBus.backupState.publishSubject.value is BackupState.BackupInProgress) {
            return
        }
        logger.i("Check backup storage status")
        EventBus.backupState.post(BackupState.BackupCheckingStorage)
        // check storage
        try {
            val backupDate = backupSettingsRepository.lastSuccessfulBackupDate!!
            if (!backupStorage.hasBackupForDate(backupDate)) {
                throw BackupStorageTamperedException("Backup storage is tampered")
            }
            when {
                backupSettingsRepository.backupFailureDate != null -> EventBus.backupState.post(BackupState.BackupOutOfDate())
                backupSettingsRepository.scheduledBackupDate?.isAfterNow == true -> EventBus.backupState.post(BackupState.BackupScheduled)
                else -> EventBus.backupState.post(BackupState.BackupUpToDate)
            }
        } catch (e: BackupStorageAuthRevokedException) {
            backupSettingsRepository.lastSuccessfulBackupDate = null
            backupSettingsRepository.backupPassword = null
            backupSettingsRepository.localBackupFolderURI = null
            backupSettingsRepository.scheduledBackupDate = null
            backupSettingsRepository.backupFailureDate = null
            EventBus.backupState.post(BackupState.BackupDisabled)
        } catch (e: BackupStorageTamperedException) {
            EventBus.backupState.post(BackupState.BackupOutOfDate(e))
        } catch (e: Exception) {
            EventBus.backupState.post(BackupState.BackupStorageCheckFailed)
            throw e
        }
    }

    fun scheduleBackup(delayMs: Long? = null, resetRetryCount: Boolean = false) {
        if (!backupSettingsRepository.backupIsEnabled) {
            return // if backup is disabled or there's a scheduled backup
        }
        if (scheduledBackupSubscription?.isDisposed == false) {
            return // if backup is disabled or there's a scheduled backup
        }
        if (resetRetryCount) {
            retryCount = 0
        }
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription =
            Observable
                .timer(delayMs ?: Constants.Wallet.backupDelayMs, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe { GlobalScope.launch(Dispatchers.IO) { runCatching { backup() } } }
        backupSettingsRepository.scheduledBackupDate = DateTime.now().plusMillis(delayMs?.toInt() ?: Constants.Wallet.backupDelayMs.toInt())
        EventBus.backupState.post(BackupState.BackupScheduled)
    }

    suspend fun backup(isInitialBackup: Boolean = false, userTriggered: Boolean = false, newPassword: CharArray? = null) {
        if (!isInitialBackup && !backupSettingsRepository.backupIsEnabled) {
            return
        }
        if (EventBus.backupState.publishSubject.value is BackupState.BackupInProgress) {
            return
        }
        // Do the work here--in this case, upload the images.
        logger.i("Backup started")
        // cancel any scheduled backup
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription = null
        retryCount++
        EventBus.backupState.post(BackupState.BackupInProgress)
        try {
            val backupDate = backupStorage.backup(newPassword)
            backupSettingsRepository.lastSuccessfulBackupDate = backupDate
            backupSettingsRepository.scheduledBackupDate = null
            backupSettingsRepository.backupFailureDate = null
            logger.i("Backup successful")
            EventBus.backupState.post(BackupState.BackupUpToDate)
        } catch (exception: Exception) {
            logger.e(exception, "Backup failed")
            if (isInitialBackup) {
                turnOff(deleteExistingBackups = true)
                retryCount = 0
                throw exception
            }
            if (exception is BackupStorageAuthRevokedException) {
                turnOff(deleteExistingBackups = true)
                retryCount = 0
                postBackupFailedNotification(exception)
                throw exception
            }
            if (userTriggered || retryCount > Constants.Wallet.maxBackupRetries) {
                logger.e(exception, "Error happened while backing up. It's a user-triggered backup or retry limit has been exceeded")
                EventBus.backupState.post(BackupState.BackupOutOfDate(exception))
                backupSettingsRepository.scheduledBackupDate = null
                backupSettingsRepository.backupFailureDate = DateTime.now()
                retryCount = 0
                if (!userTriggered) { // post notification
                    postBackupFailedNotification(exception)
                }
                throw exception
            } else {
                logger.e(exception, "Backup failed. Will schedule.")
                scheduleBackup(delayMs = Constants.Wallet.backupRetryPeriodMs, resetRetryCount = false)
            }
            logger.e(exception, "Error happened while backing up. Will retry")
        }
    }

    private fun postBackupFailedNotification(exception: Exception) {
        val title = when (exception) {
            is BackupStorageFullException -> context.getString(R.string.backup_wallet_storage_full_title)
            else -> context.getString(R.string.back_up_wallet_backing_up_error_title)
        }
        val body = when {
            exception is BackupStorageFullException -> context.getString(
                R.string.backup_wallet_storage_full_desc
            )
            exception is BackupStorageAuthRevokedException -> context.getString(
                R.string.check_backup_storage_status_auth_revoked_error_description
            )
            exception is UnknownHostException -> context.getString(R.string.error_no_connection_title)
            exception.message == null -> context.getString(R.string.back_up_wallet_backing_up_unknown_error)
            else -> context.getString(R.string.back_up_wallet_backing_up_error_desc, exception.message!!)
        }
        notificationHelper.postNotification(title = title, body = body)
    }

    // TODO(nyarian): TBD - should side effects be separated from the direct flow?
    suspend fun turnOff(deleteExistingBackups: Boolean) {
        backupSettingsRepository.lastSuccessfulBackupDate = null
        backupSettingsRepository.backupFailureDate = null
        backupSettingsRepository.backupPassword = null
        backupSettingsRepository.scheduledBackupDate = null
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription = null
        EventBus.backupState.post(BackupState.BackupDisabled)
        if (deleteExistingBackups) {
            runCatching { backupStorage.deleteAllBackupFiles() }
        }
        backupSettingsRepository.localBackupFolderURI = null
        runCatching { backupStorage.signOut() }
    }

}
