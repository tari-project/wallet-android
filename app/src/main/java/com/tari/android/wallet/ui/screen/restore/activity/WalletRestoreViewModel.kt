package com.tari.android.wallet.ui.screen.restore.activity

import com.tari.android.wallet.application.walletManager.WalletLauncher
import com.tari.android.wallet.application.walletManager.doOnWalletFailed
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import javax.inject.Inject

class WalletRestoreViewModel() : CommonViewModel() {

    @Inject
    lateinit var walletLauncher: WalletLauncher

    init {
        component.inject(this)

        launchOnIo {
            walletManager.doOnWalletFailed {
                showErrorDialog(it, onClose = { launchOnMain { resetFlow() } })
            }
        }
    }

    fun checkIfWalletRestored() {
        if (!tariSettingsSharedRepository.isRestoredWallet) {
            walletLauncher.stop()
        }
    }

    private fun resetFlow() {
        walletManager.deleteWallet()
        tariNavigator.navigate(Navigation.SplashScreen())
    }
}