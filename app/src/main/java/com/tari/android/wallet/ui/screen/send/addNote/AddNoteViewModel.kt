package com.tari.android.wallet.ui.screen.send.addNote

import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import javax.inject.Inject

class AddNoteViewModel : CommonViewModel() {

    @Inject
    lateinit var networkConnection: NetworkConnectionStateHandler

    init {
        component.inject(this)
    }

    fun emojiIdClicked(walletAddress: TariWalletAddress) {
        showAddressDetailsDialog(walletAddress)
    }

    fun continueToFinalizeSendTx(transactionData: TransactionData) {
        tariNavigator.navigate(Navigation.TxSend.ToConfirm(transactionData))
    }

    fun isNetworkConnectionAvailable(): Boolean = networkConnection.isNetworkConnected()
}