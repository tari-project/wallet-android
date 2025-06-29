package com.tari.android.wallet.ui.screen.send.obsolete.addNote

import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
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