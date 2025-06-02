package com.tari.android.wallet.data.sharedPrefs.network

import android.content.SharedPreferences
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.SimplePrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPrefRepository @Inject constructor(sharedPrefs: SharedPreferences) {
    // defaultNetwork is the network that will be used if the current network is not set or is not supported
    private val defaultNetwork = when (BuildConfig.LIB_WALLET_NETWORK) {
        "MAINNET" -> NETWORK_MAINNET
        "NEXTNET" -> NETWORK_NEXTNET
        "ESMERALDA" -> NETWORK_ESMERALDA
        else -> error("Need to define lib versions for the network in TariBuildConfig")
    }

    var supportedNetworks: List<TariNetwork> = when (BuildConfig.LIB_WALLET_NETWORK) {
        "MAINNET" -> listOf(NETWORK_MAINNET)
        "NEXTNET" -> listOf(NETWORK_NEXTNET)
        "ESMERALDA" -> listOf(NETWORK_ESMERALDA)
        else -> error("Need to define lib versions for the network in TariBuildConfig")
    }

    private var _currentNetwork: Network by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = SimplePrefRepository(this),
        name = Keys.CURRENT_NETWORK,
        type = Network::class.java,
        defValue = defaultNetwork.network,
    )
    var currentNetwork: TariNetwork
        get() = supportedNetworks.firstOrNull { it.network == _currentNetwork } ?: defaultNetwork
        set(value) {
            _currentNetwork = value.network
        }

    fun isCurrentNetworkSupported(): Boolean = supportedNetworks.any { it.network == currentNetwork.network }

    fun setDefaultNetworkAsCurrent() {
        currentNetwork = defaultNetwork
    }

    companion object {
        private const val TICKER_MAINNET = "XTM"
        private const val TICKER_TESTNET = "tXTM"

        private val NETWORK_MAINNET: TariNetwork = TariNetwork(
            network = Network.MAINNET,
            dnsPeer = "seeds.tari.com",
            blockExplorerBaseUrl = "https://explore.tari.com",
            ticker = TICKER_MAINNET,
            recommended = true,
        )
        private val NETWORK_STAGENET: TariNetwork = TariNetwork(
            network = Network.STAGENET,
            dnsPeer = "seeds.stagenet.tari.com",
            blockExplorerBaseUrl = null,
            ticker = TICKER_TESTNET,
            recommended = true,
        )
        private val NETWORK_NEXTNET: TariNetwork = TariNetwork(
            network = Network.NEXTNET,
            dnsPeer = "aurora.nextnet.tari.com",
            blockExplorerBaseUrl = "https://explore-nextnet.tari.com",
            ticker = TICKER_TESTNET,
            recommended = false,
        )
        private val NETWORK_ESMERALDA: TariNetwork = TariNetwork(
            network = Network.ESMERALDA,
            dnsPeer = "seeds.esmeralda.tari.com",
            blockExplorerBaseUrl = null,
            ticker = TICKER_TESTNET,
            recommended = false,
        )

        object Keys {
            const val CURRENT_NETWORK = "tari_current_network_type"
        }
    }
}