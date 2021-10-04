package com.tari.android.wallet.data.network

import android.content.SharedPreferences
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate

class NetworkRepositoryImpl(private val sharedPrefs: SharedPreferences) : NetworkRepository {
    private val networkPropertyName = "tari_current_network"
    private val ffiNetworkPropertyName = "ffi_tari_current_network"
    private val networkIncompatiblePropertyName = "tari_network_incompatible_current_network"
    private val mainNetThicker = "XTR"
    private val testNetThicker = "tXTR"

    override var supportedNetworks: List<Network> = listOf(Network.WEATHERWAX, Network.IGOR)

    override var currentNetwork by SharedPrefGsonDelegate(sharedPrefs, networkPropertyName, TariNetwork::class.java)

    init {
        if (currentNetwork == null) {
            currentNetwork = TariNetwork(Network.WEATHERWAX, testNetThicker)
        }
    }

    override var ffiNetwork: Network? by SharedPrefGsonDelegate(sharedPrefs, formatKey(ffiNetworkPropertyName), Network::class.java)

    override var incompatibleNetworkShown by SharedPrefBooleanDelegate(sharedPrefs, formatKey(networkIncompatiblePropertyName), false)

    override fun getAllNetworks(): List<TariNetwork> {
        return listOf(
            TariNetwork(Network.WEATHERWAX, testNetThicker),
            TariNetwork(Network.IGOR, testNetThicker)
        )
    }

    private fun formatKey(key: String) : String = key + "_" + currentNetwork!!.network.displayName
}