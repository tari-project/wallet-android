package com.tari.android.wallet.ui.fragment.send.addAmount.feeModule

sealed class NetworkSpeed {
    object Slow : NetworkSpeed()
    object Medium : NetworkSpeed()
    object Fast: NetworkSpeed()
}