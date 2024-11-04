package com.tari.android.wallet.data.sharedPrefs.network

import android.content.SharedPreferences
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.sharedPrefs.SimplePrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.util.DebugConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkPrefRepository @Inject constructor(sharedPrefs: SharedPreferences) {
    // defaultNetwork is the network that will be used if the current network is not set or is not supported
    private val defaultNetwork = if (DebugConfig.mockNetwork) NETWORK_ESMERALDA else NETWORK_NEXTNET

    var supportedNetworks: List<TariNetwork> = if (DebugConfig.mockNetwork) listOf(NETWORK_ESMERALDA) else listOf(NETWORK_NEXTNET)

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
        private const val TICKER_MAINNET = "XTR"
        private const val TICKER_TESTNET = "tXTR"

        private val NETWORK_STAGENET: TariNetwork = TariNetwork(
            network = Network.STAGENET,
            dnsPeer = "seeds.stagenet.tari.com",
            blockExplorerUrl = null,
            ticker = TICKER_TESTNET,
            recommended = true,
        )
        private val NETWORK_NEXTNET: TariNetwork = TariNetwork(
            network = Network.NEXTNET,
            dnsPeer = "seeds.nextnet.tari.com",
            blockExplorerUrl = "https://explore-nextnet.tari.com",
            ticker = TICKER_TESTNET,
            recommended = false,
        )
        private val NETWORK_ESMERALDA: TariNetwork = TariNetwork(
            network = Network.ESMERALDA,
            dnsPeer = "seeds.esmeralda.tari.com",
            blockExplorerUrl = null,
            ticker = TICKER_TESTNET,
            recommended = false,
        )

        object Keys {
            const val CURRENT_NETWORK = "tari_current_network_type"
        }
    }
}