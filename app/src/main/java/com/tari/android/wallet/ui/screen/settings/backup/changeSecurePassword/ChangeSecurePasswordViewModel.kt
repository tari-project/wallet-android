package com.tari.android.wallet.ui.screen.settings.backup.changeSecurePassword

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class ChangeSecurePasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefs: CorePrefRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSharedPrefsRepository: BackupPrefRepository

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    init {
        component.inject(this)
    }

    val backupState = backupStateHandler.backupState

    fun backToBackupSettings() {
        tariNavigator.navigate(Navigation.AllSettings.BackToBackupSettings)
    }
}