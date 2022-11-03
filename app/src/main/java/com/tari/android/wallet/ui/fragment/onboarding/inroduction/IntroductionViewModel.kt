package com.tari.android.wallet.ui.fragment.onboarding.inroduction

import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class IntroductionViewModel : CommonViewModel() {

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    init {
        component.inject(this)
    }
}