package com.tari.android.wallet.ui.fragment.settings.backup.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.dropbox.core.oauth.DbxCredential
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefDateTimeDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import org.joda.time.DateTime

class BackupSettingsRepository(private val context: Context, private val sharedPrefs: SharedPreferences, networkRepository: NetworkRepository) :
    CommonRepository(networkRepository) {

    var localFileOption: BackupOptionDto? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.localFileOptionsKey), BackupOptionDto::class.java)

    var googleDriveOption: BackupOptionDto? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.googleDriveOptionKey), BackupOptionDto::class.java)

    var dropboxOption: BackupOptionDto? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.dropboxOptionsKey), BackupOptionDto::class.java)

    var dropboxCredential: DbxCredential? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.dropboxCredentialKey), DbxCredential::class.java)

    var backupPassword: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, formatKey(Keys.backupPassword))

    var localBackupFolderURI: Uri? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.localBackupFolderURI), Uri::class.java)

    var lastBackupDialogShown: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.lastBackupDialogShownTime), null)

    init {
        localFileOption = localFileOption ?: BackupOptionDto(BackupOptions.Local)
        googleDriveOption = googleDriveOption ?: BackupOptionDto(BackupOptions.Google)
    }

    val getOptionList: List<BackupOptionDto> = listOf()
//        get() = if (BuildConfig.FLAVOR == Constants.Build.privacyFlavor) {
//            listOfNotNull(localFileOption).toList()
//        } else {
//            listOfNotNull(googleDriveOption, dropboxOption).toList()
//        }

    fun isShowHintDialog(): Boolean = with(lastBackupDialogShown) { this == null || !this.plusMinutes(delayTimeInMinutes).isAfterNow }

    fun clear() {
        lastBackupDialogShown = null
        backupPassword = null
        localBackupFolderURI = null
        localFileOption = BackupOptionDto(BackupOptions.Local)
        googleDriveOption = BackupOptionDto(BackupOptions.Google)
        dropboxOption = BackupOptionDto(BackupOptions.Dropbox)
    }

    fun updateOption(option: BackupOptionDto) {
        when (option.type) {
            BackupOptions.Google -> googleDriveOption = option
            BackupOptions.Local -> localFileOption = option
            BackupOptions.Dropbox -> dropboxOption = option
        }
    }

    fun getOptionDto(type: BackupOptions): BackupOptionDto? = when (type) {
        BackupOptions.Google -> googleDriveOption
        BackupOptions.Local -> localFileOption
        BackupOptions.Dropbox -> dropboxOption
    }

    object Keys {
        const val googleDriveOptionKey = "tari_wallet_google_drive_backup_options"
        const val localFileOptionsKey = "tari_wallet_local_file_backup_options"
        const val dropboxOptionsKey = "tari_wallet_dropbox_backup_options"
        const val dropboxCredentialKey = "tari_wallet_dropbox_credential_key"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
        const val lastBackupDialogShownTime = "last_shown_time_key"
    }

    companion object {
        const val delayTimeInMinutes = 5
    }
}