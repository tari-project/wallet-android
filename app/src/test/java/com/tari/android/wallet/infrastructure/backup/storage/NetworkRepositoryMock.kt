package com.tari.android.wallet.infrastructure.backup.storage

import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork

class NetworkRepositoryMock : NetworkRepository {
    private val network: Network = Network.DIBBLER

    override var supportedNetworks: List<Network> = listOf(network)
    override var currentNetwork: TariNetwork? = TariNetwork(network, "xtr", "")
    override var ffiNetwork: Network? = network
    override var incompatibleNetworkShown: Boolean = false
    override var recommendedNetworks: List<Network> = listOf(network)

    override fun getAllNetworks(): List<TariNetwork> = listOf()
}