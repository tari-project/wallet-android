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
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.dropbox.DropboxBackupStorage
import com.tari.android.wallet.infrastructure.backup.googleDrive.GoogleDriveBackupStorage
import com.tari.android.wallet.infrastructure.backup.local.LocalBackupStorage
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
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
    private val localFileBackupStorage: LocalBackupStorage,
    val dropboxBackupStorage: DropboxBackupStorage,
    private val googleDriveBackupStorage: GoogleDriveBackupStorage,
    private val notificationHelper: NotificationHelper
) {

    private var retryCount = 0
    var currentOption: BackupOptions? = null
    private var scheduledBackupSubscription: Disposable? = null

    init {
        val backupsState = BackupsState(backupSettingsRepository.getOptionList.associate { Pair(it.type, getBackupByOption(it)) })
        EventBus.backupState.post(backupsState)
    }

    fun setupStorage(option: BackupOptions, hostFragment: Fragment) {
        currentOption = option
        getStorageByOption(option).setup(hostFragment)
    }

    suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean =
        currentOption?.let { getStorageByOption(it).onSetupActivityResult(requestCode, resultCode, intent) } ?: false

    suspend fun checkStorageStatus() {
        for (currentBackupOption in backupSettingsRepository.getOptionList) {
            val backupsState = EventBus.backupState.publishSubject.value!!.copy()
            if (!currentBackupOption.isEnable) {
                return
            }
            if (backupsState.backupsStates[currentBackupOption.type] is BackupState.BackupInProgress) {
                return
            }
            Logger.d("Check backup storage status.")

            fun updateState(state: BackupState) {
                val newState = backupsState.copy(
                    backupsStates = backupsState.backupsStates.toMutableMap().also { it[currentBackupOption.type] = state })
                EventBus.backupState.post(newState)
            }

            updateState(BackupState.BackupCheckingStorage)
            try {
                if (!getStorageByOption(currentBackupOption.type).hasBackup()) {
                    throw BackupStorageTamperedException("Backup storage is tampered.")
                }
                when {
                    currentBackupOption.lastFailureDate != null -> updateState(BackupState.BackupOutOfDate())
                    backupSettingsRepository.scheduledBackupDate?.isAfterNow == true -> updateState(BackupState.BackupScheduled)
                    else -> updateState(BackupState.BackupUpToDate)
                }
            } catch (e: BackupStorageAuthRevokedException) {
                backupSettingsRepository.clear()
                updateState(BackupState.BackupDisabled)
            } catch (e: BackupStorageTamperedException) {
                updateState(BackupState.BackupOutOfDate(e))
            } catch (e: Exception) {
                Logger.e(e, "Error while checking storage. %s", e.toString())
                updateState(BackupState.BackupStorageCheckFailed)
                throw e
            }
        }
    }

    fun scheduleBackupAll(delayMs: Long? = null, resetRetryCount: Boolean = false) {
        backupSettingsRepository.getOptionList.forEach { scheduleBackup(it.type, delayMs, resetRetryCount) }
    }

    fun scheduleBackup(optionType: BackupOptions, delayMs: Long? = null, resetRetryCount: Boolean = false): BackupState {
        val currentDto = backupSettingsRepository.getOptionList.firstOrNull { it.type == optionType } ?: return BackupState.BackupDisabled
        if (!currentDto.isEnable) {
            Logger.i("Backup is not enabled - cannot schedule a backup.")
            return BackupState.BackupDisabled
        }
        if (scheduledBackupSubscription?.isDisposed == false) {
            Logger.i("There is already a scheduled backup - cannot schedule a backup.")
            return BackupState.BackupDisabled
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
                        try {
                            backup(optionType)
                        } catch (exception: Exception) {
                            Logger.e(exception, "Error during scheduled backup: $exception")
                        }
                    }
                }
        backupSettingsRepository.scheduledBackupDate = DateTime.now().plusMillis(delayMs?.toInt() ?: Constants.Wallet.backupDelayMs.toInt())
        Logger.i("Backup scheduled.")
        return BackupState.BackupScheduled
    }

    suspend fun backupAll(isInitialBackup: Boolean = false, userTriggered: Boolean = false, newPassword: CharArray? = null) {
        backupSettingsRepository.getOptionList.forEach { backup(it.type, isInitialBackup, userTriggered, newPassword) }
    }

    suspend fun backup(optionType: BackupOptions, isInitialBackup: Boolean = false, userTriggered: Boolean = false, newPassword: CharArray? = null) {
        val currentDto = backupSettingsRepository.getOptionList.firstOrNull { it.type == optionType } ?: return
        if (!isInitialBackup && !currentDto.isEnable) {
            Logger.d("Backup is disabled. Exit.")
            return
        }

        val backupsState = EventBus.backupState.publishSubject.value!!.copy()
        if (backupsState.backupsStates[optionType] is BackupState.BackupInProgress) {
            Logger.d("Backup is in progress. Exit.")
            return
        }

        fun updateState(state: BackupState) {
            val newState = backupsState.copy(backupsStates = backupsState.backupsStates.toMutableMap().also { it[optionType] = state })
            EventBus.backupState.post(newState)
        }

        // Do the work here--in this case, upload the images.
        Logger.d("Do backup.")
        // cancel any scheduled backup
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription = null
        retryCount++
        updateState(BackupState.BackupInProgress)
        try {
            val backupDate = getStorageByOption(optionType).backup(newPassword)
            backupSettingsRepository.updateOption(
                currentDto.copy(
                    isEnable = true,
                    lastSuccessDate = SerializableTime(backupDate),
                    lastFailureDate = null
                )
            )
            backupSettingsRepository.scheduledBackupDate = null
            Logger.d("Backup successful.")
            updateState(BackupState.BackupUpToDate)
        } catch (exception: Exception) {
            if (isInitialBackup) {
                turnOff(optionType)
                retryCount = 0
                throw exception
            }
            if (exception is BackupStorageAuthRevokedException) {
                turnOff(optionType)
                retryCount = 0
                postBackupFailedNotification(exception)
                throw exception
            }
            if (userTriggered || retryCount > Constants.Wallet.maxBackupRetries) {
                Logger.e(exception, "Error happened while backing up. It's a user-triggered backup or retry limit has been exceeded.")
                updateState(BackupState.BackupOutOfDate(exception))
                backupSettingsRepository.updateOption(currentDto.copy(lastSuccessDate = null, lastFailureDate = SerializableTime(DateTime.now())))
                retryCount = 0
                if (!userTriggered) { // post notification
                    postBackupFailedNotification(exception)
                }
                throw exception
            } else {
                Logger.e(exception, "Backup failed: ${exception.message}. Will schedule.")
                scheduleBackup(optionType, Constants.Wallet.backupRetryPeriodMs, false)
            }
            Logger.e(exception, "Error happened while backing up. Will retry.")
        }
    }

    suspend fun turnOffAll() {
        backupSettingsRepository.getOptionList.forEach { turnOff(it.type) }
    }

    suspend fun turnOff(optionType: BackupOptions) {
        val options = backupSettingsRepository.getOptionList.map {
            it.copy(type = it.type, isEnable = false, lastSuccessDate = null, lastFailureDate = null)
        }
        val backupsState = EventBus.backupState.publishSubject.value!!.copy()
        backupSettingsRepository.updateOptions(options)
        backupSettingsRepository.backupPassword = null
        scheduledBackupSubscription?.dispose()
        scheduledBackupSubscription = null
        val newState =
            backupsState.copy(backupsStates = backupsState.backupsStates.toMutableMap().also { it[optionType] = BackupState.BackupDisabled })
        EventBus.backupState.post(newState)
        val backupStorage = getStorageByOption(optionType)
        backupSettingsRepository.localBackupFolderURI = null
        backupStorage.signOut()
    }

    suspend fun signOut() {
        getStorageByOption(currentOption!!).signOut()
    }

    suspend fun restoreLatestBackup(password: String? = null) {
        getStorageByOption(currentOption!!).restoreLatestBackup(password)
    }

    private fun getBackupByOption(optionDto: BackupOptionDto): BackupState {
        val scheduledBackupDate = backupSettingsRepository.scheduledBackupDate
        return when {
            !optionDto.isEnable -> BackupState.BackupDisabled
            optionDto.lastFailureDate != null -> BackupState.BackupOutOfDate()
            scheduledBackupDate != null -> {
                val delayMs = scheduledBackupDate.millis - DateTime.now().millis
                scheduleBackup(optionDto.type, delayMs = max(0, delayMs), resetRetryCount = true)
            }
            else -> {
                BackupState.BackupUpToDate
            }
        }
    }

    private fun getStorageByOption(optionType: BackupOptions): BackupStorage = when (optionType) {
        BackupOptions.Google -> googleDriveBackupStorage
        BackupOptions.Dropbox -> dropboxBackupStorage
        BackupOptions.Local -> localFileBackupStorage
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
        notificationHelper.postNotification(title, body)
    }
}
