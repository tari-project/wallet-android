package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.ConnectionState
import com.tari.android.wallet.data.baseNode.BaseNodeState
import com.tari.android.wallet.data.baseNode.BaseNodeSyncState
import com.tari.android.wallet.data.network.NetworkConnectionState
import com.tari.android.wallet.tor.TorBootstrapStatus
import com.tari.android.wallet.tor.TorProxyState
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariModalBottomSheet
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.safeCastTo

@Composable
fun ConnectionStatusModal(
    onDismiss: () -> Unit,
    connectionState: ConnectionState,
    modifier: Modifier = Modifier,
) {
    TariModalBottomSheet(
        modifier = modifier,
        onDismiss = onDismiss,
    ) { animatedDismiss ->
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Spacer(Modifier.size(40.dp))

            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.connection_status_dialog_title),
                style = TariDesignSystem.typography.modalTitle,
            )

            Spacer(Modifier.size(30.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(connectionState.networkText()),
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
                ConnectionIconView(connectionState.networkIcon())
            }
            Spacer(Modifier.size(20.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 10.dp))
            Spacer(Modifier.size(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(connectionState.torText()),
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
                connectionState.torBootstrapPercent()?.let { percent ->
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "($percent%)",
                        style = TariDesignSystem.typography.body1,
                    )
                }
                Spacer(Modifier.weight(1f))
                ConnectionIconView(connectionState.torIcon())
            }
            Spacer(Modifier.size(20.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 10.dp))
            Spacer(Modifier.size(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(connectionState.baseNodeStateText()),
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
                Spacer(Modifier.weight(1f))
                ConnectionIconView(connectionState.baseNodeStateIcon())
            }
            Spacer(Modifier.size(20.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 10.dp))
            Spacer(Modifier.size(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(connectionState.baseNodeSyncText()),
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
                ConnectionIconView(connectionState.baseNodeSyncIcon())
            }
            Spacer(Modifier.size(20.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 10.dp))
            Spacer(Modifier.size(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.connection_status_dialog_chain_tip) + " ",
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
                Text(
                    text = when {
                        connectionState.walletScannedHeight == 0 && connectionState.chainTip == 0 -> {
                            stringResource(R.string.connection_status_dialog_chain_tip_waiting)
                        }

                        else -> stringResource(
                            R.string.connection_status_dialog_chain_tip_value,
                            connectionState.walletScannedHeight,
                            connectionState.chainTip
                        )
                    },
                    style = TariDesignSystem.typography.body1.copy(
                        color = if (connectionState.walletScannedHeight != connectionState.chainTip) {
                            TariDesignSystem.colors.errorMain
                        } else {
                            TariDesignSystem.colors.textSecondary
                        },
                    ),
                )
            }

            Spacer(Modifier.size(20.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 10.dp))
            Spacer(Modifier.size(20.dp))

            TariTextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 30.dp),
                text = stringResource(R.string.common_close),
                onClick = animatedDismiss,
            )

            Spacer(Modifier.size(40.dp))
        }
    }
}

@Composable
private fun ConnectionIconView(
    connectionIcon: ConnectionIcon,
    modifier: Modifier = Modifier,
) {
    when (connectionIcon) {
        ConnectionIcon.Success -> Image(
            modifier = modifier.size(24.dp),
            painter = painterResource(R.drawable.vector_connection_status_success),
            contentDescription = null,
        )

        ConnectionIcon.Progress -> TariProgressView(modifier = modifier.size(24.dp))

        ConnectionIcon.Failure -> Image(
            modifier = modifier.size(24.dp),
            painter = painterResource(R.drawable.vector_connection_status_failure),
            contentDescription = null,
        )
    }
}

@StringRes
private fun ConnectionState.networkText(): Int = when (networkState) {
    NetworkConnectionState.CONNECTED -> R.string.connection_status_dialog_network_status_connected
    NetworkConnectionState.UNKNOWN,
    NetworkConnectionState.DISCONNECTED -> R.string.connection_status_dialog_network_status_disconnected
}

private fun ConnectionState.networkIcon(): ConnectionIcon = when (networkState) {
    NetworkConnectionState.CONNECTED -> ConnectionIcon.Success
    NetworkConnectionState.DISCONNECTED,
    NetworkConnectionState.UNKNOWN -> ConnectionIcon.Failure
}

@StringRes
private fun ConnectionState.torText(): Int = when (torProxyState) {
    is TorProxyState.NotReady -> R.string.connection_status_dialog_tor_status_disconnected
    is TorProxyState.ReadyForWallet -> R.string.connection_status_dialog_tor_status_ready_for_wallet
    is TorProxyState.Initializing -> R.string.connection_status_dialog_tor_status_connecting
    is TorProxyState.Running -> R.string.connection_status_dialog_tor_status_connected
    is TorProxyState.Failed -> R.string.connection_status_dialog_tor_status_failed
}

private fun ConnectionState.torIcon(): ConnectionIcon = when (torProxyState) {
    is TorProxyState.NotReady -> ConnectionIcon.Failure
    is TorProxyState.ReadyForWallet -> ConnectionIcon.Progress
    is TorProxyState.Initializing -> ConnectionIcon.Progress
    is TorProxyState.Running -> ConnectionIcon.Success
    is TorProxyState.Failed -> ConnectionIcon.Failure
}

private fun ConnectionState.torBootstrapPercent(): Int? = torProxyState.safeCastTo<TorProxyState.Initializing>()?.bootstrapStatus?.progress

@StringRes
private fun ConnectionState.baseNodeStateText(): Int = when (baseNodeState) {
    BaseNodeState.Syncing -> R.string.connection_status_dialog_base_node_status_connecting
    BaseNodeState.Offline -> R.string.connection_status_dialog_base_node_status_disconnected
    BaseNodeState.Online -> R.string.connection_status_dialog_base_node_status_connected
}

private fun ConnectionState.baseNodeStateIcon(): ConnectionIcon = when (baseNodeState) {
    BaseNodeState.Syncing -> ConnectionIcon.Progress
    BaseNodeState.Offline -> ConnectionIcon.Failure
    BaseNodeState.Online -> ConnectionIcon.Success
}

@StringRes
private fun ConnectionState.baseNodeSyncText(): Int = when (baseNodeSyncState) {
    BaseNodeSyncState.NotStarted -> R.string.connection_status_dialog_base_node_sync_idle
    BaseNodeSyncState.Syncing -> R.string.connection_status_dialog_base_node_sync_pending
    BaseNodeSyncState.Online -> R.string.connection_status_dialog_base_node_sync_success
    BaseNodeSyncState.Failed -> R.string.connection_status_dialog_base_node_sync_failure
}

private fun ConnectionState.baseNodeSyncIcon(): ConnectionIcon = when (baseNodeSyncState) {
    BaseNodeSyncState.NotStarted -> ConnectionIcon.Failure
    BaseNodeSyncState.Syncing -> ConnectionIcon.Progress
    BaseNodeSyncState.Online -> ConnectionIcon.Success
    BaseNodeSyncState.Failed -> ConnectionIcon.Failure
}

private enum class ConnectionIcon { Success, Progress, Failure }

@Composable
@Preview
private fun ConnectionStatusModalPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ConnectionStatusModal(
            onDismiss = {},
            connectionState = ConnectionState(
                torProxyState = TorProxyState.Initializing(bootstrapStatus = TorBootstrapStatus(progress = 50, summary = "")),
            ),
        )
    }
}