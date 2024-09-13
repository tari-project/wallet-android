package com.tari.android.wallet.ui.fragment.send.addNote

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.common.TransactionData

class AddNoteViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    fun emojiIdClicked(walletAddress: TariWalletAddress) {
        showAddressDetailsDialog(walletAddress)
    }

    fun continueToFinalizeSendTx(newData: TransactionData) {
        tariNavigator.navigate(Navigation.AddAmountNavigation.ContinueToFinalizing(newData))
    }
}