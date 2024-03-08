package com.tari.android.wallet.data.sharedPrefs.network

import android.content.SharedPreferences
import com.tari.android.wallet.application.Network
import com.tari.android.wallet.data.repository.SimpleRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.util.DebugConfig

class NetworkRepositoryImpl(sharedPrefs: SharedPreferences) : NetworkRepository {

    override val defaultNetwork = if (DebugConfig.useStagenetNetwork) getStagenet() else getNextnet()

    override var supportedNetworks: List<TariNetwork> = if (DebugConfig.useStagenetNetwork) listOf(getStagenet()) else listOf(getNextnet())

    override var currentNetwork by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        commonRepository = SimpleRepository(this),
        name = Keys.CURRENT_NETWORK,
        type = TariNetwork::class.java,
        defValue = defaultNetwork,
    )

    override var ffiNetwork: Network? by SharedPrefGsonDelegate(
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

        fun getStagenet(): TariNetwork = TariNetwork(Network.STAGENET, TICKER_TESTNET)
        fun getNextnet(): TariNetwork = TariNetwork(Network.NEXTNET, TICKER_TESTNET, recommended = true)
    }
}