package com.tari.android.wallet.ui.screen.restore.activity

import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain

class WalletRestoreViewModel() : CommonViewModel() {

    init {
        component.inject(this)

        launchOnIo {
            walletManager.doOnWalletFailed {
                showErrorDialog(it, onClose = { launchOnMain { resetFlow() } })
            }
        }
    }

    private fun resetFlow() {
        walletManager.deleteWallet()
        tariNavigator.navigate(Navigation.SplashScreen())
    }
}