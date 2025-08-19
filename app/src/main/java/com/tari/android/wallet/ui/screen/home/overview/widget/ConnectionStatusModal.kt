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
import com.tari.android.wallet.data.network.NetworkConnectionState
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariModalBottomSheet
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

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
private fun ConnectionState.baseNodeStateText(): Int = when {
    baseNodeState.isSynced -> R.string.connection_status_dialog_base_node_status_connected
    else -> R.string.connection_status_dialog_base_node_status_connecting
}

private fun ConnectionState.baseNodeStateIcon(): ConnectionIcon = when {
    baseNodeState.isSynced -> ConnectionIcon.Success
    else -> ConnectionIcon.Progress
}

private enum class ConnectionIcon { Success, Progress, Failure }

@Composable
@Preview
private fun ConnectionStatusModalPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ConnectionStatusModal(
            onDismiss = {},
            connectionState = ConnectionState(),
        )
    }
}