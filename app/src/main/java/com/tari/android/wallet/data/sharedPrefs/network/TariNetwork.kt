package com.tari.android.wallet.data.sharedPrefs.network

import com.tari.android.wallet.application.Network

data class TariNetwork(
    val network: Network,
    val dnsPeer: String,
    val httpBaseNode: String,
    val ticker: String,
    val blockExplorerBaseUrl: String? = null,
    val recommended: Boolean = false,
) {
    val isBlockExplorerAvailable: Boolean
        get() = blockExplorerBaseUrl != null
}