package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import com.tari.android.wallet.application.MigrationManager
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class CreateWalletViewModel : CommonViewModel() {
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    private val migrationManager = MigrationManager()

    init {
        component.inject(this)

        migrationManager.updateWalletVersion()
    }
}