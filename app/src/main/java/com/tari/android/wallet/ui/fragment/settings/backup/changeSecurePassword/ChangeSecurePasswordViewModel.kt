package com.tari.android.wallet.ui.fragment.settings.backup.changeSecurePassword

import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.infrastructure.backup.BackupManager
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import javax.inject.Inject

class ChangeSecurePasswordViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefs: CorePrefRepository

    @Inject
    lateinit var backupManager: BackupManager

    @Inject
    lateinit var backupSharedPrefsRepository: BackupPrefRepository

    init {
        component.inject(this)
    }
}