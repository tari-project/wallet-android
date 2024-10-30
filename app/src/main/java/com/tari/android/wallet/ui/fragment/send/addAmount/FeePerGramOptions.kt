package com.tari.android.wallet.ui.fragment.send.addAmount

import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.fragment.send.addAmount.feeModule.NetworkSpeed

data class FeePerGramOptions(val networkSpeed: NetworkSpeed, val slow: MicroTari, val medium: MicroTari, val fast: MicroTari)

