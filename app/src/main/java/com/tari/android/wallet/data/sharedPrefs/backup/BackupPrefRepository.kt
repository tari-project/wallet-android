package com.tari.android.wallet.data.sharedPrefs.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.infrastructure.backup.BackupUtxos
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOptionDto
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
        defValue = BackupOptionDto(type = BackupOption.Local, isEnable = false, lastSuccessDate = null, lastFailureDate = null),
    )

    private var googleDriveOption: BackupOptionDto by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Keys.GOOGLE_DRIVE_OPTION_KEY),
        type = BackupOptionDto::class.java,
        defValue = BackupOptionDto(type = BackupOption.Google, isEnable = false, lastSuccessDate = null, lastFailureDate = null),
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

    val currentBackupOption: BackupOptionDto
        get() = when (BuildConfig.FLAVOR) {
            Constants.Build.privacyFlavor -> localFileOption
            Constants.Build.regularFlavor -> googleDriveOption
            else -> error("Unknown build flavor: ${BuildConfig.FLAVOR}")
        }

    fun clear() {
        backupPassword = null
        localBackupFolderURI = null
        localFileOption = BackupOptionDto(BackupOption.Local, isEnable = false, lastSuccessDate = null, lastFailureDate = null)
        googleDriveOption = BackupOptionDto(BackupOption.Google, isEnable = false, lastSuccessDate = null, lastFailureDate = null)
    }

    fun updateOption(option: BackupOptionDto) {
        when (option.type) {
            BackupOption.Google -> googleDriveOption = option
            BackupOption.Local -> localFileOption = option
        }
    }

    fun getOptionDto(type: BackupOption): BackupOptionDto = when (type) {
        BackupOption.Google -> googleDriveOption
        BackupOption.Local -> localFileOption
    }

    companion object {
        object Keys {
            const val GOOGLE_DRIVE_OPTION_KEY = "tari_wallet_google_drive_backup_options"
            const val LOCAL_FILE_OPTIONS_KEY = "tari_wallet_local_file_backup_options"
            const val BACKUP_PASSWORD = "tari_wallet_last_next_alarm_time"
            const val LOCAL_BACKUP_FOLDER_URI = "tari_wallet_local_backup_folder_uri"
            const val LAST_RESTORED_TXS = "tari_wallet_restored_txs"
        }
    }
}