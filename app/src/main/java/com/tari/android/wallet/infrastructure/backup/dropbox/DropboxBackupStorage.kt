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
package com.tari.android.wallet.infrastructure.backup.dropbox

import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.SearchMatchV2
import com.dropbox.core.v2.files.WriteMode
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.delegates.SerializableTime
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.infrastructure.backup.googleDrive.GoogleDriveBackupStorage
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class DropboxBackupStorage(
    private val context: Context,
    private val namingPolicy: BackupNamingPolicy,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val walletTempDirPath: String,
    private val backupFileProcessor: BackupFileProcessor
) : BackupStorage {

    private val logger
        get() = Logger.t(GoogleDriveBackupStorage::class.simpleName)

    private var isAuthStarted = false

    private val sDbxRequestConfig: DbxRequestConfig = DbxRequestConfig.newBuilder(context.getString(R.string.app_name))
        .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
        .build()

    init {
        backupSettingsRepository.dropboxCredential?.let {
            DropboxClientFactory.init(it, sDbxRequestConfig)
        }
    }

    override fun setup(hostFragment: Fragment) {
        startAuth(hostFragment.requireActivity())
    }

    private fun startAuth(context: Context) {
        backupSettingsRepository.dropboxCredential = null
        val permissions = listOf("account_info.read", "files.content.write", "files.content.read")
        startOAuth2Authentication(context, permissions, sDbxRequestConfig)
        isAuthStarted = true
    }

    private fun startOAuth2Authentication(context: Context, scope: List<String?>?, requestConfig: DbxRequestConfig) {
        Auth.startOAuth2PKCE(context, BuildConfig.DROPBOX_ACCESS_TOKEN, requestConfig, scope)
    }

    override suspend fun onSetupActivityResult(requestCode: Int, resultCode: Int, intent: Intent?): Boolean {
        if (!isAuthStarted) return false
        isAuthStarted = false
        //need time to dropbox UI to process deeplink intent from OAuth
        delay(500)

        val dbxCredential = Auth.getDbxCredential()
        backupSettingsRepository.dropboxCredential = dbxCredential
        if (dbxCredential != null) {
            DropboxClientFactory.init(dbxCredential, sDbxRequestConfig)
        } else {
            throw BackupStorageSetupCancelled()
        }
        return true
    }

    override suspend fun backup(): DateTime = withContext(Dispatchers.IO) {
        val (backupFile, backupDate, _) = backupFileProcessor.generateBackupFile()
        try {
            uploadDropboxFile(DropboxClientFactory.client, DRIVE_BACKUP_PARENT_FOLDER_NAME + backupFile.name, backupFile.inputStream())
        } catch (e: Throwable) {
            throw BackupException(e)
        }

        try {
            backupFileProcessor.clearTempFolder()
        } catch (e: Exception) {
            Logger.e(e, "Ignorable backup error while clearing temporary and old files.")
        }
        return@withContext backupDate
    }

    private fun uploadDropboxFile(client: DbxClientV2, path: String, inputStream: InputStream): FileMetadata =
        client.files().uploadBuilder(path).withMode(WriteMode.OVERWRITE).uploadAndFinish(inputStream)

    override suspend fun hasBackup(): Boolean {
        try {
            val fileName = namingPolicy.getBackupFileName()
            val latestBackupFileName = searchForBackups()
            return latestBackupFileName.any { it.metadata.metadataValue.name.contains(fileName) }
        } catch (exception: UserRecoverableAuthIOException) {
            throw BackupStorageAuthRevokedException()
        } catch (exception: Exception) {
            throw exception
        }
    }

    override suspend fun restoreLatestBackup(password: String?) {
        val backup = searchForBackups().firstOrNull() ?: throw BackupStorageTamperedException("Backup file not found in folder.")

        withContext(Dispatchers.IO) {
            val tempFolder = File(walletTempDirPath)
            val tempFile = File(tempFolder, backup.metadata.metadataValue.name)
            if (tempFolder.parentFile?.exists() != true) {
                tempFolder.parentFile?.mkdir()
            }
            if (!tempFolder.exists()) {
                tempFolder.mkdir()
            }
            if (!tempFile.exists()) {
                tempFile.createNewFile()
            }
            FileOutputStream(tempFile).use { targetOutputStream ->
                DropboxClientFactory.client.files().download(backup.metadata.metadataValue.pathLower).download(targetOutputStream)
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
            // restore successful, turn on automated backup
            backupSettingsRepository.dropboxOption =
                backupSettingsRepository.dropboxOption!!.copy(lastSuccessDate = SerializableTime(DateTime.now()))
            backupSettingsRepository.backupPassword = password
        }
    }

    private fun searchForBackups(): List<SearchMatchV2> =
        DropboxClientFactory.client.files().searchV2(namingPolicy.regex.pattern).matches.toList()


    override suspend fun deleteAllBackupFiles() {
        searchForBackups().forEach {
            DropboxClientFactory.client.files().deleteV2(it.metadata.metadataValue.pathLower)
        }
    }

    override suspend fun signOut() {
        backupSettingsRepository.dropboxCredential = null
    }

    private companion object {
        private const val DRIVE_BACKUP_PARENT_FOLDER_NAME = "/backup/"
    }
}