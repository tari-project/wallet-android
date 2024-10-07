package com.tari.android.wallet.data.sharedPrefs.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.dropbox.core.oauth.DbxCredential
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
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
) : CommonPrefRepository(networkRepository) {

    private var localFileOption: BackupOptionDto by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.LOCAL_FILE_OPTIONS_KEY),
        type = BackupOptionDto::class.java,
        defValue = BackupOptionDto(BackupOptions.Local),
    )

    private var googleDriveOption: BackupOptionDto by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.GOOGLE_DRIVE_OPTION_KEY),
        type = BackupOptionDto::class.java,
        defValue = BackupOptionDto(BackupOptions.Google),
    )

    var dropboxOption: BackupOptionDto? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.DROPBOX_OPTIONS_KEY),
        type = BackupOptionDto::class.java,
    )

    var dropboxCredential: DbxCredential? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.DROPBOX_CREDENTIAL_KEY),
        type = DbxCredential::class.java
    )

    var backupPassword: String? by SharedPrefStringSecuredDelegate(
        context = context,
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.BACKUP_PASSWORD),
    )

    var localBackupFolderURI: Uri? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.LOCAL_BACKUP_FOLDER_URI),
        type = Uri::class.java,
    )

    var restoredTxs: BackupUtxos? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.LAST_RESTORED_TXS),
        type = BackupUtxos::class.java,
    )

    val getOptionList: List<BackupOptionDto>
        get() = if (BuildConfig.FLAVOR == Constants.Build.privacyFlavor) {
            listOfNotNull(localFileOption)
        } else {
            listOfNotNull(googleDriveOption, dropboxOption)
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

    companion object {
        object Keys {
            const val GOOGLE_DRIVE_OPTION_KEY = "tari_wallet_google_drive_backup_options"
            const val LOCAL_FILE_OPTIONS_KEY = "tari_wallet_local_file_backup_options"
            const val DROPBOX_OPTIONS_KEY = "tari_wallet_dropbox_backup_options"
            const val DROPBOX_CREDENTIAL_KEY = "tari_wallet_dropbox_credential_key"
            const val BACKUP_PASSWORD = "tari_wallet_last_next_alarm_time"
            const val LOCAL_BACKUP_FOLDER_URI = "tari_wallet_local_backup_folder_uri"
            const val LAST_RESTORED_TXS = "tari_wallet_restored_txs"
        }
    }
}