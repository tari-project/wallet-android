package com.tari.android.wallet.ui.screen.tx.history

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariSearchField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.home.overview.widget.TxItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@Composable
fun TxHistoryScreen(
    uiState: TxHistoryViewModel.UiState,
    onSearchQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onTxClick: (tx: TxDto) -> Unit,
) {
    var searchQuery by remember { mutableStateOf(TextFieldValue(uiState.searchQuery)) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.contact_details_transaction_history),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                Spacer(Modifier.size(20.dp))
                TariSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    searchQuery = searchQuery,
                    hint = stringResource(R.string.home_search_hint),
                    onQueryChanged = {
                        onSearchQueryChange(it.text)
                        searchQuery = it
                    },
                )
            }

            item { Spacer(Modifier.size(20.dp)) }

            when {
                uiState.showEmptyState -> {
                    item {
                        EmptyState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 20.dp),
                        )
                    }
                }

                uiState.showSortedList -> {
                    txListItems(
                        txList = uiState.sortedTxList,
                        ticker = uiState.ticker,
                        onTxClick = onTxClick,
                    )
                }

                else -> {
                    txListItems(
                        txList = uiState.pendingTxs,
                        ticker = uiState.ticker,
                        titleRes = R.string.home_pending_transactions_title,
                        onTxClick = onTxClick,
                    )
                    if (uiState.pendingTxs.isNotEmpty()) item { Spacer(Modifier.size(10.dp)) }
                    txListItems(
                        txList = uiState.nonPendingTxs,
                        ticker = uiState.ticker,
                        titleRes = R.string.home_completed_transactions_title,
                        onTxClick = onTxClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.contact_details_transaction_history_empty_state_title),
            style = TariDesignSystem.typography.body1,
            textAlign = TextAlign.Center,
            color = TariDesignSystem.colors.textPrimary,
        )
        Spacer(Modifier.size(8.dp))
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(R.string.home_transaction_list_empty_description),
            style = TariDesignSystem.typography.body2,
            color = TariDesignSystem.colors.textSecondary,
        )
    }
}

private fun LazyListScope.txListItems(
    txList: List<TxDto>,
    ticker: String,
    @StringRes titleRes: Int? = null,
    onTxClick: (tx: TxDto) -> Unit,
) {
    if (txList.isEmpty()) return

    if (titleRes != null) {
        item {
            Text(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = stringResource(titleRes),
                style = TariDesignSystem.typography.headingMedium,
            )
        }
    }

    items(txList.size) { index ->
        val keyboardController = LocalSoftwareKeyboardController.current
        val txItem = txList[index]
        TxItem(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 5.dp)
                .animateItem(),
            txDto = txItem,
            ticker = ticker,
            onTxClick = {
                keyboardController?.hide()
                onTxClick(txItem)
            },
        )
    }
}

@Composable
@Preview
private fun TxHistoryScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        TxHistoryScreen(
            uiState = TxHistoryViewModel.UiState(
                pendingTxs = MockDataStub.createTxList(),
                nonPendingTxs = MockDataStub.createTxList(),
                ticker = "XTM",
            ),
            onSearchQueryChange = {},
            onBackClick = {},
            onTxClick = {},
        )
    }
}

@Composable
@Preview
private fun TxHistoryScreenEmptyPreview() {
    TariDesignSystem(TariTheme.Light) {
        TxHistoryScreen(
            uiState = TxHistoryViewModel.UiState(
                pendingTxs = emptyList(),
                nonPendingTxs = emptyList(),
                ticker = "XTM",
            ),
            onSearchQueryChange = {},
            onBackClick = {},
            onTxClick = {},
        )
    }
}
