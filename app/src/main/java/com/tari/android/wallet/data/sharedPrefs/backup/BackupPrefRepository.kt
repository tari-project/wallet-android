package com.tari.android.wallet.data.sharedPrefs.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.dropbox.core.oauth.DbxCredential
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.infrastructure.backup.BackupUtxos
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupOptions
import com.tari.android.wallet.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupPrefRepository @Inject constructor(
    context: Context,
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkPrefRepository
) : CommonRepository(networkRepository) {

    var localFileOption: BackupOptionDto? by SharedPrefGsonNullableDelegate(sharedPrefs, this,  formatKey(Keys.localFileOptionsKey), BackupOptionDto::class.java)

    var googleDriveOption: BackupOptionDto? by SharedPrefGsonNullableDelegate(sharedPrefs, this,  formatKey(Keys.googleDriveOptionKey), BackupOptionDto::class.java)

    var dropboxOption: BackupOptionDto? by SharedPrefGsonNullableDelegate(sharedPrefs, this,  formatKey(Keys.dropboxOptionsKey), BackupOptionDto::class.java)

    var dropboxCredential: DbxCredential? by SharedPrefGsonNullableDelegate(sharedPrefs, this,  formatKey(Keys.dropboxCredentialKey), DbxCredential::class.java)

    var backupPassword: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, this, formatKey(Keys.backupPassword))

    var localBackupFolderURI: Uri? by SharedPrefGsonNullableDelegate(sharedPrefs, this,  formatKey(Keys.localBackupFolderURI), Uri::class.java)

    var restoredTxs: BackupUtxos? by SharedPrefGsonNullableDelegate(sharedPrefs, this,  formatKey(Keys.lastRestoredTxs), BackupUtxos::class.java, null)

    init {
        localFileOption = localFileOption ?: BackupOptionDto(BackupOptions.Local)
        googleDriveOption = googleDriveOption ?: BackupOptionDto(BackupOptions.Google)
    }

    val getOptionList: List<BackupOptionDto>
        get() = if (BuildConfig.FLAVOR == Constants.Build.privacyFlavor) {
            listOfNotNull(localFileOption).toList()
        } else {
            listOfNotNull(googleDriveOption, dropboxOption).toList()
        }

    fun clear() {
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
        const val lastRestoredTxs = "tari_wallet_restored_txs"
    }

    companion object {
        const val delayTimeInMinutes = 5
    }
}