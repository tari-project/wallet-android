package com.tari.android.wallet.ui.screen.home.overview

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariInheritTextButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariPullToRefreshBox
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.screen.home.overview.widget.ActiveMinersCard
import com.tari.android.wallet.ui.screen.home.overview.widget.BalanceInfoModal
import com.tari.android.wallet.ui.screen.home.overview.widget.BlockSyncChip
import com.tari.android.wallet.ui.screen.home.overview.widget.EmptyTxList
import com.tari.android.wallet.ui.screen.home.overview.widget.RestoreSuccessModal
import com.tari.android.wallet.ui.screen.home.overview.widget.SyncSuccessModal
import com.tari.android.wallet.ui.screen.home.overview.widget.TxItem
import com.tari.android.wallet.ui.screen.home.overview.widget.VersionCodeChip
import com.tari.android.wallet.ui.screen.home.overview.widget.WalletBalanceCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.isTrue
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun HomeOverviewScreen(
    uiState: HomeOverviewModel.UiState,
    onPullToRefresh: () -> Unit,
    onInviteFriendClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onStartMiningClicked: () -> Unit,
    onSendTariClicked: () -> Unit,
    onRequestTariClicked: () -> Unit,
    onTxClick: (txDto: TxDto) -> Unit,
    onViewAllTxsClick: () -> Unit,
    onConnectionStatusClick: () -> Unit,
    onSyncDialogDismiss: () -> Unit,
    onBalanceInfoClicked: () -> Unit,
    onBalanceInfoDialogDismiss: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            Row(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.home_title_tari_universe),
                    style = TariDesignSystem.typography.heading2XLarge,
                )
                Spacer(Modifier.width(10.dp))
                VersionCodeChip(
                    modifier = Modifier.weight(1f),
                    networkName = uiState.networkName,
                    ffiVersion = uiState.ffiVersion,
                    connectionIndicatorState = uiState.connectionState.indicatorState,
                    onVersionClick = onConnectionStatusClick,
                )
                // TODO actions are not used yet
//                Spacer(Modifier.width(10.dp))
//                IconButton(onClick = onInviteFriendClick) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.vector_home_overview_invite_friend),
//                        contentDescription = null,
//                        tint = TariDesignSystem.colors.componentsNavbarIcons,
//                    )
//                }
//                IconButton(onClick = onNotificationsClick) {
//                    Icon(
//                        painter = painterResource(id = R.drawable.vector_home_overview_notifications),
//                        contentDescription = null,
//                        tint = TariDesignSystem.colors.componentsNavbarIcons,
//                    )
//                }
            }
        }
    ) { paddingValues ->
        TariPullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            onPullToRefresh = onPullToRefresh,
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Spacer(modifier = Modifier.height(25.dp))
                    ActiveMinersCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        activeMinersCount = uiState.activeMinersCount,
                        activeMinersCountError = uiState.activeMinersCountError,
                        isMining = uiState.isMining,
                        showMiningStatus = !uiState.isMiningError,
                        onStartMiningClicked = onStartMiningClicked,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    WalletBalanceCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        balance = uiState.balance,
                        ticker = uiState.ticker,
                        onBalanceHelpClicked = onBalanceInfoClicked,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        TariInheritTextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.send_tari_subtitle),
                            onClick = onSendTariClicked,
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        TariInheritTextButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.request_tari_subtitle),
                            onClick = onRequestTariClicked,
                        )
                    }
                }

                if (uiState.txList == null) {
                    item {
                        TariProgressView(modifier = Modifier.padding(40.dp))
                    }
                } else if (uiState.txList.isEmpty()) {
                    item {
                        EmptyTxList(
                            showStartMiningButton = !uiState.isMining.isTrue(),
                            onStartMiningClicked = onStartMiningClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 80.dp),
                        )
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 30.dp, bottom = 10.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.home_tx_list_recent_activity),
                                style = TariDesignSystem.typography.headingXLarge,
                            )
                            Spacer(Modifier.weight(1f))
                            BlockSyncChip(
                                walletScannedHeight = uiState.connectionState.walletScannedHeight,
                                chainTip = uiState.connectionState.chainTip,
                            )
                        }
                    }
                    items(uiState.txList.size) { index ->
                        val txItem = uiState.txList[index]
                        TxItem(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                                .animateItem(),
                            txDto = txItem,
                            ticker = uiState.ticker,
                            onTxClick = { onTxClick(txItem) },
                        )
                    }
                    item {
                        TariTextButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(align = Alignment.CenterHorizontally),
                            text = stringResource(R.string.home_transaction_view_all_txs),
                            onClick = onViewAllTxsClick,
                        )
                    }
                    item {
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }

        if (uiState.showWalletSyncSuccessDialog) {
            SyncSuccessModal(onDismiss = onSyncDialogDismiss)
        }

        if (uiState.showWalletRestoreSuccessDialog) {
            RestoreSuccessModal(onDismiss = onSyncDialogDismiss)
        }

        if (uiState.showBalanceInfoDialog) {
            BalanceInfoModal(
                onDismiss = onBalanceInfoDialogDismiss,
                totalBalance = uiState.balance.totalBalance,
                availableBalance = uiState.balance.availableBalance,
                ticker = uiState.ticker,
            )
        }
    }
}

@Composable
@Preview
private fun HomeOverviewScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        HomeOverviewScreen(
            uiState = HomeOverviewModel.UiState(
                activeMinersCount = 10,
                isMining = false,
                balance = BalanceInfo(
                    availableBalance = 4_836_150_000.toMicroTari(),
                    pendingIncomingBalance = 0.toMicroTari(),
                    pendingOutgoingBalance = 0.toMicroTari(),
                    timeLockedBalance = 0.toMicroTari(),
                ),
                ticker = "XTM",
                networkName = "Testnet",
                ffiVersion = "v1.11.0-rc.0",
                txList = MockDataStub.createTxList(),
            ),
            onPullToRefresh = {},
            onInviteFriendClick = {},
            onNotificationsClick = {},
            onStartMiningClicked = {},
            onSendTariClicked = {},
            onRequestTariClicked = {},
            onTxClick = {},
            onViewAllTxsClick = {},
            onConnectionStatusClick = {},
            onSyncDialogDismiss = {},
            onBalanceInfoClicked = {},
            onBalanceInfoDialogDismiss = {},
        )
    }
}
