package com.tari.android.wallet.ui.dialog.modular.modules.connection

import com.tari.android.wallet.R
import com.tari.android.wallet.data.ConnectionState
import com.tari.android.wallet.data.baseNode.BaseNodeState
import com.tari.android.wallet.data.baseNode.BaseNodeSyncState
import com.tari.android.wallet.data.network.NetworkConnectionState
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class ConnectionStatusesModule(
    val connectionState: ConnectionState,
) : IDialogModule() {

    val networkText = when (connectionState.networkState) {
        NetworkConnectionState.CONNECTED -> R.string.connection_status_dialog_network_status_connected
        NetworkConnectionState.UNKNOWN,
        NetworkConnectionState.DISCONNECTED -> R.string.connection_status_dialog_network_status_disconnected
    }

    val networkIcon = when (connectionState.networkState) {
        NetworkConnectionState.CONNECTED -> R.drawable.vector_network_status_dot_green
        NetworkConnectionState.DISCONNECTED,
        NetworkConnectionState.UNKNOWN -> R.drawable.vector_network_status_dot_red
    }

    val torText = when (connectionState.torProxyState) {
        is TorProxyState.NotReady -> R.string.connection_status_dialog_tor_status_disconnected
        is TorProxyState.ReadyForWallet -> R.string.connection_status_dialog_tor_status_ready_for_wallet
        is TorProxyState.Initializing -> R.string.connection_status_dialog_tor_status_connecting
        is TorProxyState.Running -> R.string.connection_status_dialog_tor_status_connected
        is TorProxyState.Failed -> R.string.connection_status_dialog_tor_status_failed
    }

    val torIcon = when (connectionState.torProxyState) {
        is TorProxyState.NotReady -> R.drawable.vector_network_status_dot_red
        is TorProxyState.ReadyForWallet -> R.drawable.vector_network_status_dot_yellow
        is TorProxyState.Initializing -> R.drawable.vector_network_status_dot_yellow
        is TorProxyState.Running -> R.drawable.vector_network_status_dot_green
        is TorProxyState.Failed -> R.drawable.vector_network_status_dot_red
    }

    val baseNodeStateText = when (connectionState.baseNodeState) {
        BaseNodeState.Syncing -> R.string.connection_status_dialog_base_node_status_connecting
        BaseNodeState.Offline -> R.string.connection_status_dialog_base_node_status_disconnected
        BaseNodeState.Online -> R.string.connection_status_dialog_base_node_status_connected
    }

    val baseNodeStateIcon = when (connectionState.baseNodeState) {
        BaseNodeState.Syncing -> R.drawable.vector_network_status_dot_yellow
        BaseNodeState.Offline -> R.drawable.vector_network_status_dot_red
        BaseNodeState.Online -> R.drawable.vector_network_status_dot_green
    }

    val baseNodeSyncText = when (connectionState.baseNodeSyncState) {
        BaseNodeSyncState.NotStarted -> R.string.connection_status_dialog_base_node_sync_idle
        BaseNodeSyncState.Syncing -> R.string.connection_status_dialog_base_node_sync_pending
        BaseNodeSyncState.Online -> R.string.connection_status_dialog_base_node_sync_success
        BaseNodeSyncState.Failed -> R.string.connection_status_dialog_base_node_sync_failure
    }

    val baseNodeSyncIcon = when (connectionState.baseNodeSyncState) {
        BaseNodeSyncState.NotStarted -> R.drawable.vector_network_status_dot_red
        BaseNodeSyncState.Syncing -> R.drawable.vector_network_status_dot_yellow
        BaseNodeSyncState.Online -> R.drawable.vector_network_status_dot_green
        BaseNodeSyncState.Failed -> R.drawable.vector_network_status_dot_red
    }

    val showChainTipConnecting: Boolean =
        connectionState.baseNodeState != BaseNodeState.Online || connectionState.walletScannedHeight == 0 || connectionState.chainTip == 0
}