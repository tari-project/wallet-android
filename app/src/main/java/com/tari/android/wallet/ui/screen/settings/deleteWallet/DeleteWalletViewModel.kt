package com.tari.android.wallet.ui.screen.settings.deleteWallet

import com.tari.android.wallet.R
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule

class DeleteWalletViewModel : CommonViewModel() {

    val showProgress = SingleLiveEvent<Unit>()
    val goToSplash = SingleLiveEvent<Unit>()

    init {
        component.inject(this)
    }

    fun confirmDeleteWallet() {
        showModularDialog(
            HeadModule(resourceManager.getString(R.string.delete_wallet_confirmation_title)),
            BodyModule(resourceManager.getString(R.string.delete_wallet_confirmation_description)),
            ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                onDeleteWalletClicked()
                hideDialog()
            },
            ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
        )
    }

    private fun onDeleteWalletClicked() {
        showProgress.postValue(Unit)
        launchOnIo {
            walletManager.deleteWallet()
            launchOnMain {
                goToSplash.postValue(Unit)
            }
        }
    }
}