package com.tari.android.wallet.ui.fragment.settings.backup.enterCurrentPassword

import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.backup.data.BackupSettingsRepository
import javax.inject.Inject

class EnterCurrentPasswordViewModel() : CommonViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupSettingsRepository

    init {
        component.inject(this)
    }
}