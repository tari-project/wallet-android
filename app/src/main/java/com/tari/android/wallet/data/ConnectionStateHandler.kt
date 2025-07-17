package com.tari.android.wallet.data

import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.data.network.NetworkConnectionState
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TariBaseNodeState
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.util.extension.collectFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles the connection state of the app. It contains the current state of the network, tor proxy, base node, and base node sync states.
 * It's primarily used to display the connection indicator and connection status dialog.
 */
@Singleton
class ConnectionStateHandler @Inject constructor(
    torProxyStateHandler: TorProxyStateHandler,
    baseNodeStateHandler: BaseNodeStateHandler,
    networkConnectionStateHandler: NetworkConnectionStateHandler,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    init {
        applicationScope.collectFlow(networkConnectionStateHandler.networkConnectionState) { networkState ->
            _connectionState.update { it.copy(networkState = networkState) }
        }
        applicationScope.collectFlow(torProxyStateHandler.torProxyState) { torProxyState ->
            _connectionState.update { it.copy(torProxyState = torProxyState) }
        }
        applicationScope.collectFlow(baseNodeStateHandler.walletScannedHeight) { height ->
            _connectionState.update { it.copy(walletScannedHeight = height) }
        }
        applicationScope.collectFlow(baseNodeStateHandler.baseNodeState) { baseNodeState ->
            _connectionState.update { it.copy(baseNodeState = baseNodeState) }
        }
    }
}

data class ConnectionState(
    val networkState: NetworkConnectionState = NetworkConnectionState.UNKNOWN,
    val torProxyState: TorProxyState = TorProxyState.NotReady,
    val baseNodeState: TariBaseNodeState = TariBaseNodeState(heightOfLongestChain = 0.toBigInteger()),
    val walletScannedHeight: Int = 0,
) {
    val indicatorState: ConnectionIndicatorState
        get() = when {
            networkState != NetworkConnectionState.CONNECTED ||
                    torProxyState !is TorProxyState.Running ||
                    !baseNodeState.isSynced -> ConnectionIndicatorState.Disconnected

            baseNodeState.heightOfLongestChain.toInt() != walletScannedHeight -> ConnectionIndicatorState.ConnectedWithIssues
            else -> ConnectionIndicatorState.Connected
        }

    val chainTip: Int
        get() = baseNodeState.heightOfLongestChain.toInt()
}

sealed class ConnectionIndicatorState() {
    data object Connected : ConnectionIndicatorState()
    data object ConnectedWithIssues : ConnectionIndicatorState()
    data object Disconnected : ConnectionIndicatorState()
}