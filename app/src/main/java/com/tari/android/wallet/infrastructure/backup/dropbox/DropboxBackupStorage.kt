
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.http.OkHttp3Requestor
import com.dropbox.core.json.JsonReadException
import com.dropbox.core.oauth.DbxCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.R
import com.tari.android.wallet.extension.getLastPathComponent
import com.tari.android.wallet.infrastructure.backup.*
import com.tari.android.wallet.infrastructure.backup.dropbox.DropboxClientFactory
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.suspendCoroutine

internal class DropboxBackupStorage(
    private val context: Context,
    private val namingPolicy: BackupNamingPolicy,
    private val backupSettingsRepository: BackupSettingsRepository,
    private val walletTempDirPath: String,
    private val backupFileProcessor: BackupFileProcessor
) : BackupStorage {

    val sDbxRequestConfig: DbxRequestConfig = DbxRequestConfig.newBuilder(context.getString(R.string.app_name))
    .withHttpRequestor(OkHttp3Requestor(OkHttp3Requestor.defaultOkHttpClient()))
    .build()

//    private val googleClient: GoogleSignInClient = GoogleSignIn.getClient(
//        context,
//        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//            .requestEmail()
//            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
//            .build()
//    )

    private lateinit var drive: Drive

    init {
//        val googleAccount = GoogleSignIn.getLastSignedInAccount(context)
////        if (googleAccount != null) {
////            val credential =
////                GoogleAccountCredential.usingOAuth2(
////                    context,
////                    listOf(DriveScopes.DRIVE_APPDATA)
////                ).apply { selectedAccount = googleAccount.account }
////            drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
////                .setApplicationName(context.resources.getString(R.string.app_name))
////                .build()
////        }

        DropboxClientFactory.init(BuildConfig.DROPBOX_ACCESS_TOKEN, sDbxRequestConfig)
    }



    private val USE_SLT = true //If USE_SLT is set to true, our Android example
    private fun startOAuth2Authentication(context: Context?, app_key: String?, scope: List<String?>?, requestConfig: DbxRequestConfig) {
        if (USE_SLT) {
            Auth.startOAuth2PKCE(context, app_key, requestConfig, scope)
        } else {
            Auth.startOAuth2Authentication(context, app_key)
        }
    }

    override fun setup(hostFragment: Fragment) {
        startOAuth2Authentication(
            hostFragment.requireContext(),
            BuildConfig.DROPBOX_ACCESS_TOKEN,
            listOf("account_info.read", "files.content.write"),
            sDbxRequestConfig
        )
//        val userActivity = Intent(hostFragment.requireContext(), UserActivity::class.java)
//        hostFragment.startActivityForResult(userActivity, REQUEST_CODE_SIGN_IN)
    }



    fun onResume(context: Context) {
        val prefs = context.getSharedPreferences("dropbox-sample", AppCompatActivity.MODE_PRIVATE)
        if (USE_SLT) {
            val serailizedCredental = prefs.getString("credential", null)
            if (serailizedCredental == null) {
                val credential = Auth.getDbxCredential()
                if (credential != null) {
                    prefs.edit().putString("credential", credential.toString()).apply()
                    DropboxClientFactory.init(credential, sDbxRequestConfig)
                }
            } else {
                try {
                    val credential = DbxCredential.Reader.readFully(serailizedCredental)
                    DropboxClientFactory.init(credential, sDbxRequestConfig)
                } catch (e: JsonReadException) {
                    throw IllegalStateException("Credential data corrupted: " + e.message)
                }
            }
        } else {
            var accessToken = prefs.getString("access-token", null)
            if (accessToken == null) {
                accessToken = Auth.getOAuth2Token()
                if (accessToken != null) {
                    prefs.edit().putString("access-token", accessToken).apply()
                    DropboxClientFactory.init(accessToken, sDbxRequestConfig)
                }
            } else {
                DropboxClientFactory.init(accessToken, sDbxRequestConfig)
            }
        }
        val uid = Auth.getUid()
        val storedUid = prefs.getString("user-id", null)
        if (uid != null && uid != storedUid) {
            prefs.edit().putString("user-id", uid).apply()
        }
    }


    protected fun hasToken(): Boolean {
//        val prefs = getSharedPreferences("dropbox-sample", AppCompatActivity.MODE_PRIVATE)
//        return if (USE_SLT) {
//            prefs.getString("credential", null) != null
//        } else {
//            val accessToken = prefs.getString("access-token", null)
//            accessToken != null
//        }
        return true
    }


    override suspend fun onSetupActivityResult(
        requestCode: Int,
        resultCode: Int,
        intent: Intent?
    ) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> when (resultCode) {
                Activity.RESULT_OK -> drive = getDrive(intent)
                Activity.RESULT_CANCELED -> throw BackupStorageSetupCancelled()
                else -> throw BackupStorageSetupException("Google Drive setup error.")
            }
        }
    }

    private suspend fun getDrive(intent: Intent?): Drive {
        return suspendCoroutine { continuation ->
            GoogleSignIn.getSignedInAccountFromIntent(intent)
                .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                    val credential = GoogleAccountCredential.usingOAuth2(
                        context,
                        listOf(DriveScopes.DRIVE_APPDATA)
                    ).apply { selectedAccount = googleAccount.account }
                    val drive = Drive.Builder(NetHttpTransport(), GsonFactory(), credential)
                        .setApplicationName(context.resources.getString(R.string.app_name))
                        .build()
                    continuation.resumeWith(Result.success(drive))
                }
                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
                .addOnCanceledListener {
                    continuation.resumeWith(
                        Result.failure(BackupInterruptedException(""))
                    )
                }
        }
    }

    override suspend fun backup(newPassword: CharArray?): DateTime {
        return withContext(Dispatchers.IO) {
            val (backupFile, backupDate, mimeType) = backupFileProcessor.generateBackupFile(
                newPassword
            )
            // upload file
            try {
                createBackupFile(backupFile, mimeType)
            } catch (exception: UserRecoverableAuthIOException) {
                throw BackupStorageAuthRevokedException()
            } catch (exception: GoogleJsonResponseException) {
                for (error in exception.details.errors) {
                    if (error.reason == "storageQuotaExceeded") {
                        throw BackupStorageFullException()
                    }
                }
                throw exception
            } catch (exception: Exception) {
                throw exception
            }
            // update backup password
            if (newPassword != null) {
                backupSettingsRepository.backupPassword = newPassword.toString()
            }
            try {
                backupFileProcessor.clearTempFolder()
                // delete older backups
                deleteAllBackupFiles(excludeBackupWithDate = backupDate)
            } catch (e: Exception) {
                Logger.e(
                    e,
                    "Ignorable backup error while clearing temporary and old files."
                )
            }
            return@withContext backupDate
        }
    }

    private fun createBackupFile(
        file: File,
        mimeType: String
    ) {
        val metadata: com.google.api.services.drive.model.File =
            com.google.api.services.drive.model.File()
                .setParents(listOf(DRIVE_BACKUP_PARENT_FOLDER_NAME))
                .setMimeType(mimeType)
                .setName(file.getLastPathComponent()!!)
        drive.files()
            .create(metadata, FileContent(mimeType, file))
            .setFields("id")
            .execute()
            ?: throw IOException("Null result when requesting file creation.")
    }

    override suspend fun hasBackupForDate(date: DateTime): Boolean {
        try {
            val latestBackupFileName = getLastBackupFileIdAndName()?.second ?: return false
            return latestBackupFileName.contains(namingPolicy.getBackupFileName(date))
        } catch (exception: UserRecoverableAuthIOException) {
            throw BackupStorageAuthRevokedException()
        } catch (exception: Exception) {
            throw exception
        }
    }

    override suspend fun restoreLatestBackup(password: String?) {
        val (backupFileId, backupFileName) = try {
            getLastBackupFileIdAndName()
        } catch (e: UserRecoverableAuthIOException) {
            throw BackupStorageAuthRevokedException()
        } catch (e: Exception) {
            throw e
        } ?: throw BackupStorageTamperedException("Backup file not found in folder.")
        withContext(Dispatchers.IO) {
            val tempFolder = File(walletTempDirPath)
            if (!tempFolder.exists()) tempFolder.mkdir()
            // copy file to temp location
            val tempFile = File(tempFolder, backupFileName)
            // create file & fetch if it hasn't been fetched before
            if (!tempFile.exists()) {
                tempFile.createNewFile()
                FileOutputStream(tempFile).use { targetOutputStream ->
                    drive.files().get(backupFileId).executeMediaAndDownloadTo(targetOutputStream)
                }
            }
            backupFileProcessor.restoreBackupFile(tempFile, password)
            backupFileProcessor.clearTempFolder()
            // restore successful, turn on automated backup
            val lastSuccessfulDate = namingPolicy.getDateFromBackupFileName(tempFile.name)
            backupSettingsRepository.dropboxOption = backupSettingsRepository.dropboxOption!!.copy(lastSuccessDate = lastSuccessfulDate)
            backupSettingsRepository.backupPassword = password
        }
    }

    private fun getLastBackupFileIdAndName(): Pair<String, String>? {
        val backups = mutableListOf<Pair<DateTime, com.google.api.services.drive.model.File>>()
        var pageToken: String? = null
        do {
            val result: FileList = searchForBackups(pageToken)
            result.files.forEach {
                namingPolicy.getDateFromBackupFileName(it.name)
                    ?.let { time -> backups.add(time to it) }
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
        val latestBackupFile = backups.maxByOrNull { it.first }?.second
        return if (latestBackupFile != null) {
            (latestBackupFile.id to latestBackupFile.name)
        } else {
            null
        }
    }

    private fun searchForBackups(pageToken: String?): FileList =
        drive.files().list()
            .setSpaces(DRIVE_BACKUP_PARENT_FOLDER_NAME)
            .setQ("'${DRIVE_BACKUP_PARENT_FOLDER_NAME}' in parents")
            .setFields("nextPageToken, files(id, name)")
            .setPageToken(pageToken)
            .execute()

    override suspend fun deleteAllBackupFiles() {
        deleteAllBackupFiles(excludeBackupWithDate = null)
    }

    private fun deleteAllBackupFiles(excludeBackupWithDate: DateTime?) {
        val driveFiles = drive.files()
        var pageToken: String? = null
        do {
            val result: FileList = searchForBackups(pageToken)
            result.files.forEach { file ->
                val excludeName: String? = if (excludeBackupWithDate != null) {
                    namingPolicy.getBackupFileName(excludeBackupWithDate)
                } else {
                    null
                }
                if (excludeName == null || !file.name.contains(excludeName)) {
                    namingPolicy.getDateFromBackupFileName(file.name)?.let {
                        driveFiles.delete(file.id).execute()
                    }
                }
            }
            pageToken = result.nextPageToken
        } while (pageToken != null)
    }

    override suspend fun signOut() {
//        suspendCoroutine<Unit> { continuation ->
//            backupFileProcessor.clearTempFolder()
//            googleClient.signOut()
//                .addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
//                .addOnCompleteListener { continuation.resumeWith(Result.success(Unit)) }
//        }
    }

    private companion object {
        private const val DRIVE_BACKUP_PARENT_FOLDER_NAME = "appDataFolder"
        private const val REQUEST_CODE_SIGN_IN = 1356
    }
}