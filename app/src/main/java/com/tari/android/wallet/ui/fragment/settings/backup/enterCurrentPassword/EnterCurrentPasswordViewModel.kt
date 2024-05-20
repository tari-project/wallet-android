package com.tari.android.wallet.ui.fragment.settings.backup.enterCurrentPassword

import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import javax.inject.Inject

class EnterCurrentPasswordViewModel() : CommonViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    init {
        component.inject(this)
    }
}