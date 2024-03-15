package com.tari.android.wallet.data.sharedPrefs.network

import com.tari.android.wallet.application.Network

data class TariNetwork(
    val network: Network,
    val dnsPeer: String,
    val ticker: String,
    val recommended: Boolean = false,
)