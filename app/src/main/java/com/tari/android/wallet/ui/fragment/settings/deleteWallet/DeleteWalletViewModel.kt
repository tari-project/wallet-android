package com.tari.android.wallet.ui.fragment.settings.deleteWallet

import com.tari.android.wallet.R
import com.tari.android.wallet.service.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import javax.inject.Inject

class DeleteWalletViewModel : CommonViewModel() {

    val deleteWallet = SingleLiveEvent<Unit>()

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    init {
        component.inject(this)
    }

    fun confirmDeleteWallet() {
        val args = ModularDialogArgs(
            DialogArgs(), listOf(
                HeadModule(resourceManager.getString(R.string.delete_wallet_confirmation_title)),
                BodyModule(resourceManager.getString(R.string.delete_wallet_confirmation_description)),
                ButtonModule(resourceManager.getString(R.string.common_confirm), ButtonStyle.Warning) {
                    deleteWallet.postValue(Unit)
                    dismissDialog.postValue(Unit)
                },
                ButtonModule(resourceManager.getString(R.string.common_cancel), ButtonStyle.Close)
            )
        )
        modularDialog.postValue(args)
    }
}