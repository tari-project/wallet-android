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
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.infrastructure.backup.dropbox.DropboxBackupStorage
import com.tari.android.wallet.infrastructure.backup.googleDrive.GoogleDriveBackupStorage
import com.tari.android.wallet.infrastructure.backup.local.LocalBackupStorage
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOption
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.CoroutineScope
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
    private val backupSettingsRepository: BackupPrefRepository,
    private val localFileBackupStorage: LocalBackupStorage,
    private val googleDriveBackupStorage: GoogleDriveBackupStorage,
    private val dropboxBackupStorage: DropboxBackupStorage,
    private val notificationHelper: NotificationHelper,
    private val walletManager: WalletManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val logger
        get() = Logger.t(BackupManager::class.simpleName)

    var currentOption: BackupOption? = BackupOption.Dropbox

    private val backupMutex = Mutex()

    private val trigger = BehaviorSubject.create<Unit>()
    private val debouncedJob = trigger.debounce(300L, TimeUnit.MILLISECONDS) // TODO don't use rx for debounce
        .doOnEach { applicationScope.launch { backupAll() } }
        .subscribe()

    init {
        val backupsState = BackupsState(backupSettingsRepository.getOptionList.associate { Pair(it.type, getBackupStateByOption(it)) })
        EventBus.backupState.post(backupsState)

        EventBus.subscribe<Event.App.AppBackgrounded>(this) { trigger.onNext(Unit) }
        EventBus.subscribe<Event.App.AppForegrounded>(this) { trigger.onNext(Unit) }

        applicationScope.launch {
            walletManager.walletEvent.collect { event ->
                when (event) {
                    is WalletEvent.Tx.TxReceived,
                    is WalletEvent.Tx.TxReplyReceived,
                    is WalletEvent.Tx.TxFinalized,
                    is WalletEvent.Tx.InboundTxBroadcast,
                    is WalletEvent.Tx.OutboundTxBroadcast,
                    is WalletEvent.Tx.TxMined,
                    is WalletEvent.Tx.TxMinedUnconfirmed,
                    is WalletEvent.Tx.TxFauxConfirmed,
                    is WalletEvent.Tx.TxFauxMinedUnconfirmed,
                    is WalletEvent.Tx.DirectSendResult,
                    is WalletEvent.Tx.TxCancelled -> trigger.onNext(Unit)

                    is WalletEvent.OnWalletRemove -> turnOffAll()
                }
            }
        }
    }

    fun setupStorage(option: BackupOption, hostFragment: Fragment) {
        currentOption = option
        getStorageByOption(option).setup(hostFragment)
    }

    suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean =
        currentOption?.let { getStorageByOption(it).onSetupActivityResult(requestCode, resultCode, intent) } ?: false

    fun backupNow() = trigger.onNext(Unit)

    private suspend fun backupAll() = backupSettingsRepository.getOptionList.forEach { backup(it.type) }

    private suspend fun backup(optionType: BackupOption) = backupMutex.withLock {
        val currentDto = backupSettingsRepository.getOptionList.firstOrNull { it.type == optionType } ?: return
        if (!currentDto.isEnable) {
            logger.d("Backup is disabled. Exit.")
            return
        }

        val backupsState = EventBus.backupState.publishSubject.value!!.copy()
        if (backupsState.backupsStates[optionType] is BackupState.BackupInProgress) {
            logger.d("Backup is in progress. Exit.")
            return
        }

        fun updateState(state: BackupState) {
            val newState = backupsState.copy(backupsStates = backupsState.backupsStates.toMutableMap().also { it[optionType] = state })
            EventBus.backupState.post(newState)
        }

        logger.i("Backup started")
        updateState(BackupState.BackupInProgress)
        try {
            val backupDate = getStorageByOption(optionType).backup()
            backupSettingsRepository.updateOption(
                currentDto.copy(
                    isEnable = true,
                    lastSuccessDate = SerializableTime(backupDate),
                    lastFailureDate = null
                )
            )
            logger.i("Backup successful")
            updateState(BackupState.BackupUpToDate)
        } catch (exception: Throwable) {
            logger.i("Backup failed $exception")
            if (exception is BackupStorageAuthRevokedException) {
                logger.i("Error happened on backup BackupStorageAuthRevokedException")
                turnOff(optionType)
                postBackupFailedNotification(exception)
            }
            logger.i("Error happened while backing up")
            updateState(BackupState.BackupFailed(exception))
            backupSettingsRepository.updateOption(currentDto.copy(lastSuccessDate = null, lastFailureDate = SerializableTime(DateTime.now())))
        }
    }

    fun turnOffAll() = applicationScope.launch {
        backupSettingsRepository.getOptionList.forEach { turnOff(it.type) }
    }

    fun turnOff(optionType: BackupOption) = with(backupMutex) {
        val backupsState = EventBus.backupState.publishSubject.value!!.copy()
        backupSettingsRepository.updateOption(BackupOptionDto(optionType))
        backupSettingsRepository.backupPassword = null
        val newState =
            backupsState.copy(backupsStates = backupsState.backupsStates.toMutableMap().also { it[optionType] = BackupState.BackupDisabled })
        EventBus.backupState.post(newState)
        val backupStorage = getStorageByOption(optionType)
        applicationScope.launch { backupStorage.signOut() }
    }

    suspend fun signOut() {
        getStorageByOption(currentOption!!).signOut()
    }

    suspend fun restoreLatestBackup(password: String? = null) = backupMutex.withLock {
        getStorageByOption(currentOption!!).restoreLatestBackup(password)
    }

    private fun getBackupStateByOption(optionDto: BackupOptionDto): BackupState = when {
        !optionDto.isEnable -> BackupState.BackupDisabled
        optionDto.lastFailureDate != null -> BackupState.BackupFailed()
        else -> BackupState.BackupUpToDate
    }

    private fun getStorageByOption(optionType: BackupOption): BackupStorage = when (optionType) {
        BackupOption.Google -> googleDriveBackupStorage
        BackupOption.Local -> localFileBackupStorage
        BackupOption.Dropbox -> dropboxBackupStorage
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
