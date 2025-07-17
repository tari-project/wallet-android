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
import com.tari.android.wallet.application.AppStateHandler
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.infrastructure.backup.googleDrive.GoogleDriveBackupStorage
import com.tari.android.wallet.infrastructure.backup.local.LocalBackupStorage
import com.tari.android.wallet.notification.NotificationHelper
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOptionDto
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
    private val notificationHelper: NotificationHelper,
    private val walletManager: WalletManager,
    private val appStateHandler: AppStateHandler,
    private val backupStateHandler: BackupStateHandler,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {

    private val logger
        get() = Logger.t(BackupManager::class.simpleName)

    val currentOption: BackupOptionDto
        get() = backupSettingsRepository.currentBackupOption

    private val backupMutex = Mutex()

    private val trigger = BehaviorSubject.create<Unit>()
    private val debouncedJob = trigger.debounce(300L, TimeUnit.MILLISECONDS) // TODO don't use rx for debounce
        .doOnEach { applicationScope.launch { backup() } }
        .subscribe()

    init {
        backupStateHandler.updateBackupState(getBackupStateByOption(currentOption))

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
                    is WalletEvent.Tx.TxCancelled -> trigger.onNext(Unit)

                    is WalletEvent.OnWalletRemove -> turnOff()

                    else -> Unit
                }
            }
        }

        applicationScope.launch {
            appStateHandler.appEvent.collect { event ->
                when (event) {
                    is AppStateHandler.AppEvent.AppBackgrounded,
                    is AppStateHandler.AppEvent.AppForegrounded,
                    is AppStateHandler.AppEvent.AppDestroyed -> trigger.onNext(Unit)
                }
            }
        }
    }

    fun setupStorage(hostFragment: Fragment) {
        currentOption.getStorage().setup(hostFragment)
    }

    suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean =
        currentOption.getStorage().onSetupActivityResult(requestCode, resultCode, intent)

    fun backupNow() = trigger.onNext(Unit)

    private suspend fun backup() = backupMutex.withLock {
        if (!currentOption.isEnable) {
            logger.d("Backup is disabled. Exit.")
            // TODO do we need logs?
            return
        }

        if (backupStateHandler.inProgress) {
            logger.d("Backup is in progress. Exit.")
            return
        }

        logger.i("Backup started")
        backupStateHandler.updateBackupState(BackupState.BackupInProgress)
        try {
            val backupDate = currentOption.getStorage().backup()
            backupSettingsRepository.updateOption(
                currentOption.copy(
                    isEnable = true,
                    lastSuccessDate = SerializableTime(backupDate),
                    lastFailureDate = null
                )
            )
            logger.i("Backup successful")
            backupStateHandler.updateBackupState(BackupState.BackupUpToDate)
        } catch (exception: Throwable) {
            logger.i("Backup failed $exception")
            if (exception is BackupStorageAuthRevokedException) {
                logger.i("Error happened on backup BackupStorageAuthRevokedException")
                turnOff()
                postBackupFailedNotification(exception)
            }
            logger.i("Error happened while backing up")
            backupStateHandler.updateBackupState(BackupState.BackupFailed(exception))
            backupSettingsRepository.updateOption(currentOption.copy(lastSuccessDate = null, lastFailureDate = SerializableTime(DateTime.now())))
        }
    }

    fun turnOff() = with(backupMutex) {
        backupSettingsRepository.updateOption(
            currentOption.copy(
                isEnable = false,
                lastSuccessDate = null,
                lastFailureDate = null,
            )
        )
        backupSettingsRepository.backupPassword = null
        backupStateHandler.updateBackupState(BackupState.BackupDisabled)
        applicationScope.launch { currentOption.getStorage().signOut() }
    }

    suspend fun signOut() {
        currentOption.getStorage().signOut()
    }

    suspend fun restoreLatestBackup(password: String? = null) = backupMutex.withLock {
        currentOption.getStorage().restoreLatestBackup(password)
    }

    private fun getBackupStateByOption(optionDto: BackupOptionDto): BackupState = when {
        !optionDto.isEnable -> BackupState.BackupDisabled
        optionDto.lastFailureDate != null -> BackupState.BackupFailed()
        else -> BackupState.BackupUpToDate
    }

    private fun BackupOptionDto.getStorage(): BackupStorage = when (this.type) {
        BackupOption.Google -> googleDriveBackupStorage
        BackupOption.Local -> localFileBackupStorage
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
