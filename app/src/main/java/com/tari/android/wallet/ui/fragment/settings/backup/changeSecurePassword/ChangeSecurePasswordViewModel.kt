package com.tari.android.wallet.ui.fragment.settings.backup.changeSecurePassword

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import javax.inject.Inject

class ChangeSecurePasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefs: SharedPrefsRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSharedPrefsRepository: BackupSettingsRepository

    init {
        component.inject(this)
    }
}