package com.tari.android.wallet.ui.dialog.error

import com.tari.android.wallet.R.string.dibbler_faucet_url
import com.tari.android.wallet.model.WalletError

class WalletErrorArgs(val error: WalletError) {
    val title: String
        get() = error.signature

    val description: Int
    //todo
        get() = dibbler_faucet_url
}