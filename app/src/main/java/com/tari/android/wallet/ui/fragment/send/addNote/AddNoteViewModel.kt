package com.tari.android.wallet.ui.fragment.send.addNote

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.network.NetworkConnectionStateHandler
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.home.navigation.Navigation
import com.tari.android.wallet.ui.fragment.send.common.TransactionData
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

    fun continueToFinalizeSendTx(newData: TransactionData) {
        tariNavigator.navigate(Navigation.AddAmountNavigation.ContinueToFinalizing(newData))
    }

    fun isNetworkConnectionAvailable(): Boolean = networkConnection.isNetworkConnected()
}