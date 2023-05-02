package com.tari.android.wallet.data.sharedPrefs.network

import android.content.SharedPreferences
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate

class NetworkRepositoryImpl(sharedPrefs: SharedPreferences) : NetworkRepository {

    override var supportedNetworks: List<Network> = listOf(Network.NEXTNET)

    override var recommendedNetworks: List<Network> = listOf(Network.NEXTNET)

    override var currentNetwork by SharedPrefGsonDelegate(sharedPrefs, Keys.currentNetwork, TariNetwork::class.java)

    init {
        try {
            currentNetwork!!.network.displayName
        } catch (e: Throwable) {
            currentNetwork = getNextnet()
        }
    }

    override var ffiNetwork: Network? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.ffiNetwork), Network::class.java)

    override var incompatibleNetworkShown by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Keys.networkIncompatible), false)

    override fun getAllNetworks(): List<TariNetwork> = listOf(getNextnet())

    object Keys {
        const val currentNetwork = "tari_current_network"
        const val ffiNetwork = "ffi_tari_current_network"
        const val networkIncompatible = "tari_network_incompatible_current_network"
    }

    companion object {
        private const val mainNetThicker = "XTR"
        private const val testNetThicker = "tXTR"

        fun getNextnet(): TariNetwork = TariNetwork(Network.NEXTNET, testNetThicker)
    }
}