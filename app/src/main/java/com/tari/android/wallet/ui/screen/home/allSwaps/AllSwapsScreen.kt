package com.tari.android.wallet.ui.screen.home.allSwaps

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariPullToRefreshBox
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.home.overview.widget.PendingExolixTxItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@Composable
fun AllSwapsScreen(
    uiState: AllSwapsViewModel.UiState,
    onBackClick: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRetry: () -> Unit,
    onSwapClick: (Exolix.Transaction) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.all_swaps_title),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        TariPullToRefreshBox(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            onPullToRefresh = onPullToRefresh,
        ) {
            TariLoadingLayout(
                modifier = Modifier.fillMaxSize(),
                targetLoadingState = uiState.loadingState,
                errorLayout = {
                    TariErrorView(
                        modifier = Modifier.fillMaxSize(),
                        onTryAgainClick = onRetry,
                    )
                },
                loadingLayout = {
                    TariProgressView(modifier = Modifier.fillMaxSize())
                },
            ) {
                if (uiState.swaps.isEmpty()) {
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.all_swaps_empty_message),
                        style = TariDesignSystem.typography.body2,
                        color = TariDesignSystem.colors.textSecondary,
                    )
                } else {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                        items(uiState.swaps) { swap ->
                            PendingExolixTxItem(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .animateItem(),
                                transaction = swap,
                                onClick = { onSwapClick(swap) },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        item {
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun AllSwapsScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        AllSwapsScreen(
            uiState = AllSwapsViewModel.UiState(
                swaps = listOf(
                    MockDataStub.createExchangeTransaction(
                        status = Exolix.TransactionStatus.WAIT,
                    ),
                    MockDataStub.createExchangeTransaction(
                        status = Exolix.TransactionStatus.CONFIRMATION,
                    ),
                    MockDataStub.createExchangeTransaction(
                        status = Exolix.TransactionStatus.OVERDUE,
                    ),
                ),
                loadingState = TariLoadingLayoutState.Content,
            ),
            onBackClick = {},
            onPullToRefresh = {},
            onRetry = {},
            onSwapClick = {},
        )
    }
}
