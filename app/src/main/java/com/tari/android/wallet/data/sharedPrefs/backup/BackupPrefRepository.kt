package com.tari.android.wallet.data.sharedPrefs.backup

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.infrastructure.backup.BackupUtxos
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupPrefRepository @Inject constructor(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    private var googleDriveOption: BackupOption by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Keys.GOOGLE_DRIVE_OPTION_KEY),
        type = BackupOption::class.java,
        defValue = BackupOption(isEnabled = false, lastSuccessDate = null, lastFailureDate = null),
    )

    var backupPassword: String? by SharedPrefStringSecuredDelegate(
        context = context,
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Keys.BACKUP_PASSWORD),
    )

    var localBackupFolderURI: Uri? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Keys.LOCAL_BACKUP_FOLDER_URI),
        type = Uri::class.java,
    )

    var restoredTxs: BackupUtxos? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Keys.LAST_RESTORED_TXS),
        type = BackupUtxos::class.java,
    )

    val currentBackupOption: BackupOption
        get() = googleDriveOption

    fun clear() {
        backupPassword = null
        localBackupFolderURI = null
        googleDriveOption = BackupOption(isEnabled = false, lastSuccessDate = null, lastFailureDate = null)
    }

    fun updateOption(option: BackupOption) {
        googleDriveOption = option
    }

    companion object {
        object Keys {
            const val GOOGLE_DRIVE_OPTION_KEY = "tari_wallet_google_drive_backup_options"
            const val BACKUP_PASSWORD = "tari_wallet_last_next_alarm_time"
            const val LOCAL_BACKUP_FOLDER_URI = "tari_wallet_local_backup_folder_uri"
            const val LAST_RESTORED_TXS = "tari_wallet_restored_txs"
        }
    }
}