package com.tari.android.wallet.ui.fragment.onboarding.createWallet

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.infrastructure.Tracker
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class CreateWalletViewModel() : CommonViewModel() {
    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var tracker: Tracker

    init {
        component.inject(this)
    }
}