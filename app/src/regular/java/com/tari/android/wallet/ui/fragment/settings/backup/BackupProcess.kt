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
package com.tari.android.wallet.ui.fragment.settings.backup

import androidx.lifecycle.*
import com.orhanobut.logger.Logger
import com.tari.android.wallet.infrastructure.backup.WalletBackup
import com.tari.android.wallet.infrastructure.backup.storage.BackupStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class StorageBackupViewModel(private val storage: BackupStorage, private val backup: WalletBackup) :
    ViewModel() {

    private val _state = MutableLiveData<StorageBackupState>()
    val state: LiveData<StorageBackupState> get() = _state
    private val currentState get() = _state.value!!

    init {
        _state.value = StorageBackupState.checkingBackupStatus()
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val status = withContext(Dispatchers.IO) {
                    if (storage.backupExists()) StorageBackupStatus.BACKED_UP
                    else StorageBackupStatus.NOT_BACKED_UP
                }
                _state.value = currentState.copy(backupStatus = status)
            } catch (e: Exception) {
                Logger.e(e, "Error occurred during backup check")
                _state.value = currentState.copy(
                    backupStatus = StorageBackupStatus.STATUS_CHECK_FAILURE,
                    processException = e
                )
            }
        }
    }

    fun backup(key: CharArray) {
        if (currentState.processStatus == BackupProcessStatus.BACKING_UP) {
            throw IllegalStateException("Backup is already in progress")
        } else {
            _state.value = currentState.copy(processStatus = BackupProcessStatus.BACKING_UP)
            viewModelScope.launch(Dispatchers.IO) {
                runBackup(key)
            }
        }
    }

    private suspend fun runBackup(key: CharArray) {
        var backupFile: File? = null
        try {
            backupFile = backup.run(key)
            storage.addBackup(backupFile)
            backupFile.delete()
            withContext(Dispatchers.Main) {
                _state.value =
                    currentState.copy(
                        backupStatus = StorageBackupStatus.BACKED_UP,
                        processStatus = BackupProcessStatus.SUCCESS
                    )
            }
        } catch (e: Exception) {
            Logger.e(e, "Error occurred during backup")
            backupFile?.delete()
            withContext(Dispatchers.Main) {
                _state.value = currentState.copy(
                    processStatus = BackupProcessStatus.FAILURE,
                    processException = e
                )
            }
        }
    }

    fun clearStatusCheckFailure() {
        if (currentState.backupStatus == StorageBackupStatus.STATUS_CHECK_FAILURE) {
            _state.value = currentState.copy(
                backupStatus = StorageBackupStatus.UNKNOWN,
                statusCheckException = null
            )
        }
    }

    fun resetProcessStatus() {
        if (currentState.processStatus == BackupProcessStatus.SUCCESS ||
            currentState.processStatus == BackupProcessStatus.FAILURE
        ) {
            _state.value =
                currentState.copy(processStatus = BackupProcessStatus.IDLE, processException = null)
        }
    }

}

data class StorageBackupState(
    val backupStatus: StorageBackupStatus,
    val statusCheckException: Exception?,
    val processStatus: BackupProcessStatus,
    val processException: Exception?
) {
    companion object {
        fun checkingBackupStatus() = StorageBackupState(
            StorageBackupStatus.CHECKING_STATUS,
            null,
            BackupProcessStatus.IDLE,
            null
        )

    }
}

enum class StorageBackupStatus { CHECKING_STATUS, STATUS_CHECK_FAILURE, NOT_BACKED_UP, BACKED_UP, UNKNOWN }

enum class BackupProcessStatus { IDLE, BACKING_UP, SUCCESS, FAILURE }

class StorageBackupViewModelFactory(
    private val storage: BackupStorage,
    private val walletBackup: WalletBackup
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T =
        if (modelClass === StorageBackupViewModel::class.java)
            @Suppress("UNCHECKED_CAST")
            StorageBackupViewModel(storage, walletBackup) as T
        else
            throw IllegalArgumentException("Expected ${StorageBackupViewModel::class.java}")

}
