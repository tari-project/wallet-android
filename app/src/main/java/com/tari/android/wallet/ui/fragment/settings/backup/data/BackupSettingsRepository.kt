package com.tari.android.wallet.ui.fragment.settings.backup.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefDateTimeDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import org.joda.time.DateTime

class BackupSettingsRepository(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val networkRepository: NetworkRepository
) {

    var localFileOption: BackupOptionDto? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.localFileOptionsKey), BackupOptionDto::class.java)

    var googleDriveOption: BackupOptionDto? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.googleDriveOptionKey), BackupOptionDto::class.java)

    var dropboxOption: BackupOptionDto? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.dropBoxOptionKey), BackupOptionDto::class.java)

    var scheduledBackupDate: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.scheduledBackupDate))

    var backupPassword: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, formatKey(Keys.backupPassword))

    var localBackupFolderURI: Uri? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.localBackupFolderURI), Uri::class.java)

    var lastBackupDialogShown: DateTime? by SharedPrefDateTimeDelegate(sharedPrefs, formatKey(Keys.lastBackupDialogShownTime), null)

    init {
        localFileOption = localFileOption ?: BackupOptionDto(BackupOptions.Local)
        googleDriveOption = googleDriveOption ?: BackupOptionDto(BackupOptions.Google)
        dropboxOption = dropboxOption ?: BackupOptionDto(BackupOptions.Dropbox)
    }

    val getOptionList: List<BackupOptionDto>
        get() = listOfNotNull(localFileOption, googleDriveOption, dropboxOption).toList()

    fun isShowHintDialog(): Boolean = with(lastBackupDialogShown) { this == null || !this.plusMinutes(delayTimeInMinutes).isAfterNow }

    fun clear() {
        scheduledBackupDate = null
        lastBackupDialogShown = null
        backupPassword = null
        localBackupFolderURI = null
        localFileOption = null
        googleDriveOption = null
        localFileOption = BackupOptionDto(BackupOptions.Local)
        googleDriveOption = BackupOptionDto(BackupOptions.Google)
        dropboxOption = BackupOptionDto(BackupOptions.Dropbox)
    }

    fun updateOptions(options: List<BackupOptionDto>) = options.forEach { option ->
        when (option.type) {
            BackupOptions.Dropbox -> dropboxOption = option
            BackupOptions.Google -> googleDriveOption = option
            BackupOptions.Local -> localFileOption = option
        }
    }

    private fun formatKey(key: String): String = key + "_" + networkRepository.currentNetwork!!.network.displayName

    object Keys {
        const val googleDriveOptionKey = "tari_wallet_google_drive_backup_options"
        const val dropBoxOptionKey = "tari_wallet_dropbox_backup_options"
        const val localFileOptionsKey = "tari_wallet_local_file_backup_options"
        const val scheduledBackupDate = "tari_wallet_scheduled_backup_date"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
        const val lastBackupDialogShownTime = "last_shown_time_key"
    }

    companion object {
        const val delayTimeInMinutes = 5
    }
}