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
package com.tari.android.wallet.infrastructure.backup.storage

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.tari.android.wallet.infrastructure.backup.BackupInterruptedException
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.suspendCoroutine
import com.google.api.services.drive.model.File as GDriveFile


interface BackupStorage {

    suspend fun addBackup(file: File)

    fun backupExists(): Boolean

    fun downloadBackup(destination: File)

}

class BackupStorageFactory(private val appName: String) {

    suspend fun google(context: Context, signInResult: Intent?): BackupStorage =
        suspendCoroutine { continuation ->
            GoogleSignIn.getSignedInAccountFromIntent(signInResult)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                    val credential =
                        GoogleAccountCredential.usingOAuth2(
                            context,
                            listOf(DriveScopes.DRIVE_APPDATA)
                        )
                            .apply { selectedAccount = googleAccount.account }
                    val drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
                        .setApplicationName(appName)
                        .build()
                    val storage = GDriveBackupStorage(drive, Executors.newSingleThreadExecutor())
                    continuation.resumeWith(Result.success(storage))
                }
                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                .addOnCanceledListener {
                    continuation.resumeWith(Result.failure(BackupInterruptedException()))
                }
        }

}

private class GDriveBackupStorage(private val drive: Drive, private val executor: Executor) :
    BackupStorage {

    private val policy = TariBackupNameValidationPolicy

    override suspend fun addBackup(file: File) {
        suspendCoroutine<Unit> { continuation ->
            createBackupFile(file)
                .addOnSuccessListener { continuation.resumeWith(Result.success(Unit)) }
                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                .addOnCanceledListener {
                    continuation.resumeWith(Result.failure(BackupInterruptedException()))
                }
        }
    }

    private fun createBackupFile(file: File): Task<GDriveFile> = Tasks.call(executor, Callable {
        val backupTime =
            TariBackupNameValidationPolicy.DATE_FORMAT.print(DateTime(DateTimeZone.UTC))
        val metadata: GDriveFile = GDriveFile()
            .setParents(listOf(DRIVE_BACKUP_PARENT_FOLDER_NAME))
            .setMimeType(BACKUP_MIME_TYPE)
            .setName("${DRIVE_BACKUP_FILE_NAME}$backupTime.${file.extension}")
        drive.files().create(metadata, FileContent(BACKUP_MIME_TYPE, file)).setFields("id")
            .execute()
            ?: throw IOException("Null result when requesting file creation.")
    })

    private fun retrieveLatestBackupId(): String? {
        val backups = mutableListOf<Pair<DateTime, GDriveFile>>()
        var pageToken: String? = null
        do {
            val result: FileList = searchForBackups(pageToken)
            result.files.forEach {
                policy.getDate(it.name)?.let { time -> backups.add(time to it) }
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
        return backups.maxBy { it.first }?.second?.id
    }

    private fun searchForBackups(pageToken: String?): FileList =
        drive.files().list()
            .setSpaces(DRIVE_BACKUP_PARENT_FOLDER_NAME)
            .setQ("mimeType='$BACKUP_MIME_TYPE' and '$DRIVE_BACKUP_PARENT_FOLDER_NAME' in parents")
            .setFields("nextPageToken, files(id, name)")
            .setPageToken(pageToken)
            .execute()

    override fun backupExists(): Boolean = retrieveLatestBackupId()?.isNotBlank() ?: false

    override fun downloadBackup(destination: File) = drive.files().get(retrieveLatestBackupId())
        .executeMediaAndDownloadTo(FileOutputStream(destination))

    private companion object {
        private const val DRIVE_BACKUP_PARENT_FOLDER_NAME = "appDataFolder"
        private const val DRIVE_BACKUP_FILE_NAME = "Tari-Aurora-Backup-"
        private const val BACKUP_MIME_TYPE = "application/zip"
    }

}

internal object TariBackupNameValidationPolicy {

    //    Tari-Aurora-Backup_yyyy-MM-dd_HH-mm-ss.*
    private val regex =
        Regex("Tari-Aurora-Backup-(\\d{4}-(((0)[1-9])|((1)[0-2]))-((0)[1-9]|[1-2][0-9]|(3)[0-1])_([0-1][0-9]|(2)[0-3])-([0-5][0-9])-([0-5][0-9]))\\..*")
    val DATE_FORMAT: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd_HH-mm-ss")

    fun getDate(name: String): DateTime? =
        regex.find(name)?.let { it.groups[1]!!.value }?.let { DATE_FORMAT.parseDateTime(it) }

}
