package com.tari.android.wallet.data.sharedPrefs.network

import android.content.SharedPreferences
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.repository.SimpleRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonNullableDelegate
import com.tari.android.wallet.util.DebugConfig

class NetworkRepositoryImpl(sharedPrefs: SharedPreferences) : NetworkRepository {

    override val defaultNetwork = if (DebugConfig.mockNetwork) NETWORK_ESMERALDA else NETWORK_STAGENET

    override var supportedNetworks: List<TariNetwork> = if (DebugConfig.mockNetwork) listOf(NETWORK_ESMERALDA) else listOf(NETWORK_STAGENET)

    override var currentNetwork by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = SimpleRepository(this),
        name = Keys.CURRENT_NETWORK,
        type = TariNetwork::class.java,
        defValue = defaultNetwork,
    )

    override var ffiNetwork: Network? by SharedPrefGsonNullableDelegate(
        prefs = sharedPrefs,
        commonRepository = SimpleRepository(this),
        name = formatKey(Keys.FFI_NETWORK),
        type = Network::class.java,
    )

    object Keys {
        const val CURRENT_NETWORK = "tari_current_network"
        const val FFI_NETWORK = "ffi_tari_current_network"
    }

    companion object {
        private const val TICKER_MAINNET = "XTR"
        private const val TICKER_TESTNET = "tXTR"

        private val NETWORK_STAGENET: TariNetwork = TariNetwork(
            network = Network.STAGENET,
            dnsPeer = "seeds.stagenet.tari.com",
            ticker = TICKER_TESTNET,
            recommended = true,
        )
        private val NETWORK_NEXTNET: TariNetwork = TariNetwork(
            network = Network.NEXTNET,
            dnsPeer = "seeds.nextnet.tari.com",
            ticker = TICKER_TESTNET,
        )
        private val NETWORK_ESMERALDA: TariNetwork = TariNetwork(
            network = Network.ESMERALDA,
            dnsPeer = "seeds.esmeralda.tari.com",
            ticker = TICKER_TESTNET,
            recommended = true,
        )
    }
}