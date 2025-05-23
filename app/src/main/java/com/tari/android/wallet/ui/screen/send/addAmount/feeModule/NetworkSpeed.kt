package com.tari.android.wallet.ui.screen.send.addAmount.feeModule

sealed class NetworkSpeed {
    object Slow : NetworkSpeed()
    object Medium : NetworkSpeed()
    object Fast: NetworkSpeed()
}