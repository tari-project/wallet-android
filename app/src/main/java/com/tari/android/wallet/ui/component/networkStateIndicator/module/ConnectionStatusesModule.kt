package com.tari.android.wallet.ui.component.networkStateIndicator.module

import com.tari.android.wallet.R
import com.tari.android.wallet.network.NetworkConnectionState
import com.tari.android.wallet.service.baseNode.BaseNodeState
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class ConnectionStatusesModule(
    val networkState: NetworkConnectionState,
    val torState: TorProxyState,
    val baseNodeState: BaseNodeState,
    val baseNodeSyncState: BaseNodeSyncState
) : IDialogModule() {

    val networkText = when (networkState) {
        NetworkConnectionState.CONNECTED -> R.string.connection_status_dialog_network_status_connected
        NetworkConnectionState.UNKNOWN,
        NetworkConnectionState.DISCONNECTED -> R.string.connection_status_dialog_network_status_disconnected
    }

    val networkIcon = when (networkState) {
        NetworkConnectionState.CONNECTED -> R.drawable.ic_network_status_dot_green
        NetworkConnectionState.DISCONNECTED,
        NetworkConnectionState.UNKNOWN -> R.drawable.ic_network_status_dot_red
    }

    val torText = when (torState) {
        TorProxyState.NotReady -> R.string.connection_status_dialog_tor_status_disconnected
        is TorProxyState.Initializing -> R.string.connection_status_dialog_tor_status_connecting
        is TorProxyState.Running -> R.string.connection_status_dialog_tor_status_connected
        is TorProxyState.Failed -> R.string.connection_status_dialog_tor_status_failed
    }

    val torIcon = when (torState) {
        TorProxyState.NotReady -> R.drawable.ic_network_status_dot_red
        is TorProxyState.Initializing -> R.drawable.ic_network_status_dot_yellow
        is TorProxyState.Running -> R.drawable.ic_network_status_dot_green
        is TorProxyState.Failed -> R.drawable.ic_network_status_dot_red
    }

    val baseNodeStateText = when (baseNodeState) {
        BaseNodeState.Syncing -> R.string.connection_status_dialog_base_node_status_connecting
        BaseNodeState.Offline -> R.string.connection_status_dialog_base_node_status_disconnected
        BaseNodeState.Online -> R.string.connection_status_dialog_base_node_status_connected
    }

    val baseNodeStateIcon = when (baseNodeState) {
        BaseNodeState.Syncing -> R.drawable.ic_network_status_dot_yellow
        BaseNodeState.Offline -> R.drawable.ic_network_status_dot_red
        BaseNodeState.Online -> R.drawable.ic_network_status_dot_green
    }

    val baseNodeSyncText = when (baseNodeSyncState) {
        BaseNodeSyncState.NotStarted -> R.string.connection_status_dialog_base_node_sync_idle
        BaseNodeSyncState.Syncing -> R.string.connection_status_dialog_base_node_sync_pending
        BaseNodeSyncState.Online -> R.string.connection_status_dialog_base_node_sync_success
        BaseNodeSyncState.Failed -> R.string.connection_status_dialog_base_node_sync_failure
    }

    val baseNodeSyncIcon = when (baseNodeSyncState) {
        BaseNodeSyncState.NotStarted -> R.drawable.ic_network_status_dot_red
        BaseNodeSyncState.Syncing -> R.drawable.ic_network_status_dot_yellow
        BaseNodeSyncState.Online -> R.drawable.ic_network_status_dot_green
        BaseNodeSyncState.Failed -> R.drawable.ic_network_status_dot_red
    }
}