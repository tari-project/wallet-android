package com.tari.android.wallet.data

import com.tari.android.wallet.R
import com.tari.android.wallet.application.baseNodes.BaseNodesManager
import com.tari.android.wallet.data.baseNode.BaseNodeState
import com.tari.android.wallet.data.baseNode.BaseNodeStateHandler
import com.tari.android.wallet.data.baseNode.BaseNodeSyncState
import com.tari.android.wallet.data.network.NetworkConnectionState
import com.tari.android.wallet.data.network.NetworkConnectionStateHandler
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.tor.TorProxyStateHandler
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.combineToPair
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
    baseNodesManager: BaseNodesManager,
    networkConnectionStateHandler: NetworkConnectionStateHandler,
    baseNodeStateHandler: BaseNodeStateHandler,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val _connectionState = MutableStateFlow(ConnectionState())
    val connectionState = _connectionState.asStateFlow()

    init {
        applicationScope.collectFlow(networkConnectionStateHandler.networkConnectionState) { networkState ->
            _connectionState.update { it.copy(networkState = networkState) }
        }
        applicationScope.collectFlow(baseNodeStateHandler.baseNodeState) { baseNodeState ->
            _connectionState.update { it.copy(baseNodeState = baseNodeState) }
        }
        applicationScope.collectFlow(baseNodeStateHandler.baseNodeSyncState) { syncState ->
            _connectionState.update { it.copy(baseNodeSyncState = syncState) }
        }
        applicationScope.collectFlow(torProxyStateHandler.torProxyState) { torProxyState ->
            _connectionState.update { it.copy(torProxyState = torProxyState) }
        }

        applicationScope.collectFlow(baseNodesManager.walletScannedHeight.combineToPair(baseNodesManager.networkBlockHeight)) { (height, tip) ->
            _connectionState.update {
                it.copy(
                    walletScannedHeight = height,
                    chainTip = tip.toInt(),
                )
            }
        }
    }
}

data class ConnectionState(
    val networkState: NetworkConnectionState = NetworkConnectionState.UNKNOWN,
    val torProxyState: TorProxyState = TorProxyState.NotReady,
    val baseNodeState: BaseNodeState = BaseNodeState.Offline,
    val baseNodeSyncState: BaseNodeSyncState = BaseNodeSyncState.NotStarted,
    val walletScannedHeight: Int = 0,
    val chainTip: Int = 0,
) {
    val indicatorState: ConnectionIndicatorState
        get() = when (networkState) {
            NetworkConnectionState.UNKNOWN,
            NetworkConnectionState.DISCONNECTED -> ConnectionIndicatorState.Disconnected

            NetworkConnectionState.CONNECTED -> {
                when (torProxyState) {
                    is TorProxyState.Failed,
                    is TorProxyState.Initializing,
                    is TorProxyState.NotReady -> ConnectionIndicatorState.Disconnected

                    is TorProxyState.Running -> {
                        when (baseNodeState) {
                            BaseNodeState.Online,
                            BaseNodeState.Syncing -> {
                                when (baseNodeSyncState) {
                                    BaseNodeSyncState.Online,
                                    BaseNodeSyncState.Syncing -> ConnectionIndicatorState.Connected

                                    else -> ConnectionIndicatorState.ConnectedWithIssues
                                }
                            }

                            else -> ConnectionIndicatorState.Disconnected
                        }
                    }

                    else -> ConnectionIndicatorState.Disconnected
                }
            }
        }
}

sealed class ConnectionIndicatorState(val resId: Int) {
    data object Connected : ConnectionIndicatorState(R.drawable.vector_network_state_full)
    data object ConnectedWithIssues : ConnectionIndicatorState(R.drawable.vector_network_state_limited)
    data object Disconnected : ConnectionIndicatorState(R.drawable.vector_network_state_off)
}