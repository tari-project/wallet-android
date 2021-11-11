package com.tari.android.wallet.data.network

import android.content.SharedPreferences
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.ui.common.domain.ResourceManager

class NetworkRepositoryImpl(private val resourceManager: ResourceManager, sharedPrefs: SharedPreferences) : NetworkRepository {

    override var supportedNetworks: List<Network> = listOf(Network.WEATHERWAX, Network.IGOR)

    override var recommendedNetworks: List<Network> = listOf(Network.WEATHERWAX)

    override var currentNetwork by SharedPrefGsonDelegate(sharedPrefs, Keys.currentNetwork, TariNetwork::class.java)

    init {
        if (currentNetwork == null) {
            currentNetwork = getWeatherwax(resourceManager)
        }
    }

    override var ffiNetwork: Network? by SharedPrefGsonDelegate(sharedPrefs, formatKey(Keys.ffiNetwork), Network::class.java)

    override var incompatibleNetworkShown by SharedPrefBooleanDelegate(sharedPrefs, formatKey(Keys.networkIncompatible), false)

    override fun getAllNetworks(): List<TariNetwork> = listOf(
        getWeatherwax(resourceManager),
        getIgor()
    )

    private fun formatKey(key: String): String = key + "_" + currentNetwork!!.network.displayName

    object Keys {
        const val currentNetwork = "tari_current_network"
        const val ffiNetwork = "ffi_tari_current_network"
        const val networkIncompatible = "tari_network_incompatible_current_network"
    }

    companion object {
        private const val mainNetThicker = "XTR"
        private const val testNetThicker = "tXTR"

        fun getWeatherwax(resourceManager: ResourceManager): TariNetwork =
            TariNetwork(Network.WEATHERWAX, resourceManager.getString(R.string.network_faucet_url), testNetThicker)

        fun getIgor(): TariNetwork = TariNetwork(Network.IGOR, null, testNetThicker)
    }
}