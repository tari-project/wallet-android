package com.tari.android.wallet.ui.fragment.settings.backup.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.dropbox.core.oauth.DbxCredential
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.infrastructure.backup.BackupUtxos
import com.tari.android.wallet.util.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupSettingsRepository @Inject constructor(
    context: Context,
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkRepository
) : CommonRepository(networkRepository) {

    var localFileOption: BackupOptionDto? by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.localFileOptionsKey),
        type = BackupOptionDto::class.java,
        defValue = BackupOptionDto(BackupOptionType.Local),
    )

    var googleDriveOption: BackupOptionDto? by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.googleDriveOptionKey),
        type = BackupOptionDto::class.java,
        defValue = BackupOptionDto(BackupOptionType.Google),
    )

    var dropboxOption: BackupOptionDto? by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.dropboxOptionsKey),
        type = BackupOptionDto::class.java,
        defValue = BackupOptionDto(BackupOptionType.Dropbox),
    )

    var dropboxCredential: DbxCredential? by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.dropboxCredentialKey),
        type = DbxCredential::class.java,
    )

    var backupPassword: String? by SharedPrefStringSecuredDelegate(
        context = context,
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.backupPassword),
    )

    var localBackupFolderURI: Uri? by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.localBackupFolderURI),
        type = Uri::class.java,
    )

    var restoredTxs: BackupUtxos? by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.lastRestoredTxs),
        type = BackupUtxos::class.java,
        defValue = null,
    )

    val optionList: List<BackupOptionDto>
        get() = if (BuildConfig.FLAVOR == Constants.Build.privacyFlavor) {
            listOfNotNull(localFileOption).toList()
        } else {
            listOfNotNull(googleDriveOption, dropboxOption).toList()
        }

    fun findOption(optionType: BackupOptionType): BackupOptionDto = optionList.find { it.type == optionType }
        ?: error("Impossible backup option $optionType")

    fun clear() {
        backupPassword = null
        localBackupFolderURI = null
        localFileOption = BackupOptionDto(BackupOptionType.Local)
        googleDriveOption = BackupOptionDto(BackupOptionType.Google)
        dropboxOption = BackupOptionDto(BackupOptionType.Dropbox)
    }

    fun updateOption(option: BackupOptionDto) {
        when (option.type) {
            BackupOptionType.Google -> googleDriveOption = option
            BackupOptionType.Local -> localFileOption = option
            BackupOptionType.Dropbox -> dropboxOption = option
        }
    }

    fun getOptionDto(type: BackupOptionType): BackupOptionDto = when (type) {
        BackupOptionType.Google -> googleDriveOption
        BackupOptionType.Local -> localFileOption
        BackupOptionType.Dropbox -> dropboxOption
    } ?: error("The $type option")

    object Keys {
        const val googleDriveOptionKey = "tari_wallet_google_drive_backup_options"
        const val localFileOptionsKey = "tari_wallet_local_file_backup_options"
        const val dropboxOptionsKey = "tari_wallet_dropbox_backup_options"
        const val dropboxCredentialKey = "tari_wallet_dropbox_credential_key"
        const val backupPassword = "tari_wallet_last_next_alarm_time"
        const val localBackupFolderURI = "tari_wallet_local_backup_folder_uri"
        const val lastRestoredTxs = "tari_wallet_restored_txs"
    }
}