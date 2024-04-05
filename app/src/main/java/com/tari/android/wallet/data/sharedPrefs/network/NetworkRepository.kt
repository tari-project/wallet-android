package com.tari.android.wallet.data.sharedPrefs.network

import com.tari.android.wallet.application.Network

interface NetworkRepository {
    // defaultNetwork is the network that will be used if the current network is not set or is not supported
    val defaultNetwork: TariNetwork

    var supportedNetworks: List<TariNetwork>

    var currentNetwork: TariNetwork

    var ffiNetwork: Network?

    fun isCurrentNetworkSupported(): Boolean = supportedNetworks.any { it.network == currentNetwork.network }

    fun setDefaultNetworkAsCurrent() {
        currentNetwork = defaultNetwork
    }
}