package com.tari.android.wallet.data.network

import com.tari.android.wallet.application.Network

interface NetworkRepository {
    var lastNetwork: Network

    var currentNetwork: TariNetwork?

    var ffiNetwork: Network?

    var incompatibleNetworkShown : Boolean

    fun getAllNetworks(): List<TariNetwork>
}