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
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.dropbox.DropboxBackupStorage
import com.tari.android.wallet.infrastructure.backup.googleDrive.GoogleDriveBackupStorage
import com.tari.android.wallet.infrastructure.backup.local.LocalBackupStorage
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionType
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.joda.time.DateTime
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    private val context: Context,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val localFileBackupStorage: LocalBackupStorage,
    private val googleDriveBackupStorage: GoogleDriveBackupStorage,
    private val dropboxBackupStorage: DropboxBackupStorage,
    private val notificationHelper: NotificationHelper
) {

    private val logger
        get() = Logger.t(BackupManager::class.simpleName)

    private val coroutineContext = Job()
    private var localScope = CoroutineScope(coroutineContext)

    private val backupMutex = Mutex()

    private val trigger = BehaviorSubject.create<Unit>()
    private val debouncedJob = trigger.debounce(300L, TimeUnit.MILLISECONDS)
        .doOnEach { localScope.launch { backupAll() } }
        .subscribe()

    init {
        val backupsState = BackupsState(backupSettingsRepository.optionList.associate { dto -> Pair(dto.type, dto.toBackupState()) })
        EventBus.backupState.post(backupsState)

        EventBus.subscribe<Event.App.AppBackgrounded>(this) { trigger.onNext(Unit) }
        EventBus.subscribe<Event.App.AppForegrounded>(this) { trigger.onNext(Unit) }
    }

    fun setupStorage(optionType: BackupOptionType, hostFragment: Fragment) {
        optionType.getStorage().setup(hostFragment)
    }

    suspend fun onSetupActivityResult(optionType: BackupOptionType, requestCode: Int, resultCode: Int, intent: Intent?): Boolean =
        optionType.getStorage().onSetupActivityResult(requestCode, resultCode, intent)

    fun backupNow() = trigger.onNext(Unit)

    private suspend fun backupAll() = backupSettingsRepository.optionList.forEach { backup(it.type) }

    private suspend fun backup(optionType: BackupOptionType) = backupMutex.withLock {
        val currentDto = backupSettingsRepository.findOption(optionType)
        if (!currentDto.isEnabled) {
            logger.i("Backup is disabled for $optionType. Exit.")
            return
        }

        val backupsState = EventBus.backupState.publishSubject.value!!.copy()
        if (backupsState.backupsStates[optionType] is BackupState.BackupInProgress) {
            logger.i("Backup is in progress for $optionType. Exit.")
            return
        }

        fun updateState(state: BackupState) {
            val newState = backupsState.copy(backupsStates = backupsState.backupsStates.toMutableMap().also { it[optionType] = state })
            EventBus.backupState.post(newState)
        }

        logger.i("Backup started for $optionType")
        updateState(BackupState.BackupInProgress)
        try {
            val backupDate = optionType.getStorage().backup()
            backupSettingsRepository.updateOption(
                currentDto.copy(
                    isEnabled = true,
                    lastSuccessDate = SerializableTime(backupDate),
                    lastFailureDate = null
                )
            )
            logger.i("Backup successful for $optionType")
            updateState(BackupState.BackupUpToDate)
        } catch (exception: Throwable) {
            logger.e("Backup failed for $optionType: ${exception.message}")
            if (exception is BackupStorageAuthRevokedException) {
                logger.e("BackupStorageAuthRevokedException happened during backup: ${exception.message}")
                turnOff(optionType)
                postBackupFailedNotification(exception)
            }
            updateState(BackupState.BackupFailed(exception))
            backupSettingsRepository.updateOption(currentDto.copy(lastSuccessDate = null, lastFailureDate = SerializableTime(DateTime.now())))
        }
    }

    fun turnOffAll() = localScope.launch {
        backupSettingsRepository.optionList.forEach { turnOff(it.type) }
    }

    fun turnOff(optionType: BackupOptionType) = with(backupMutex) {
        val backupsState = EventBus.backupState.publishSubject.value!!.copy()
        backupSettingsRepository.updateOption(BackupOptionDto(optionType))
        backupSettingsRepository.backupPassword = null
        val newState =
            backupsState.copy(backupsStates = backupsState.backupsStates.toMutableMap().also { it[optionType] = BackupState.BackupDisabled })
        EventBus.backupState.post(newState)
        localScope.launch { optionType.getStorage().signOut() }
    }

    suspend fun signOut(optionType: BackupOptionType) {
        optionType.getStorage().signOut()
    }

    suspend fun restoreLatestBackup(optionType: BackupOptionType, password: String? = null) = backupMutex.withLock {
        optionType.getStorage().restoreLatestBackup(password)
    }

    private fun BackupOptionDto.toBackupState(): BackupState = when {
        !this.isEnabled -> BackupState.BackupDisabled
        this.lastFailureDate != null -> BackupState.BackupFailed()
        else -> BackupState.BackupUpToDate
    }

    private fun BackupOptionType.getStorage(): BackupStorage = when (this) {
        BackupOptionType.Google -> googleDriveBackupStorage
        BackupOptionType.Local -> localFileBackupStorage
        BackupOptionType.Dropbox -> dropboxBackupStorage
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
