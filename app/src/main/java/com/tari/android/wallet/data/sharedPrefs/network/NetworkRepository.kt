package com.tari.android.wallet.data.sharedPrefs.network

import com.tari.android.wallet.application.Network

interface NetworkRepository {
    var supportedNetworks: List<Network>

    var currentNetwork: TariNetwork?

    var ffiNetwork: Network?

    var incompatibleNetworkShown : Boolean

    fun getAllNetworks(): List<TariNetwork>
}