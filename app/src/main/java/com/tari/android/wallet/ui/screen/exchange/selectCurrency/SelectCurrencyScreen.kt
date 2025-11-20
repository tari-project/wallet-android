package com.tari.android.wallet.ui.screen.exchange.selectCurrency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariSearchField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.compose.widgets.InfiniteListHandler
import com.tari.android.wallet.ui.screen.exchange.widget.CurrencyItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@Composable
fun SelectCurrencyScreen(
    uiState: SelectCurrencyViewModel.UiState,
    onBackClick: () -> Unit,
    onSearchQueryChange: (query: String) -> Unit,
    onCurrencyItemClick: (currency: CurrencyDto) -> Unit,
    onLoadMoreCurrencies: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.exchange_select_currency_title),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        var searchQuery by remember { mutableStateOf(TextFieldValue(uiState.searchQuery)) }

        val listState = rememberLazyListState()

        InfiniteListHandler(
            listState = listState,
            buffer = 5,
            onLoadMore = onLoadMoreCurrencies,
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Spacer(Modifier.size(20.dp))
                TariSearchField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    searchQuery = searchQuery,
                    hint = stringResource(R.string.exchange_search_currency_hint),
                    onQueryChanged = {
                        searchQuery = it
                        onSearchQueryChange(it.text)
                    },
                    onSearchClicked = { focusManager.clearFocus() },
                )
                Spacer(Modifier.size(16.dp))
            }

            if (uiState.showEmptyState) {
                item {
                    EmptyState(Modifier.fillMaxSize())
                }
            } else {
                items(uiState.currencies) { currency ->
                    CurrencyItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                        title = currency.currency.code + ": " + currency.currency.name,
                        subtitle = currency.network.name,
                        iconUrl = currency.iconUrl,
                        selected = currency == uiState.selectedCurrency,
                        onClick = { onCurrencyItemClick(currency) },
                    )
                }

                item {
                    LoadingItem(
                        modifier = Modifier.fillMaxWidth(),
                        uiState = uiState,
                        onReloadClick = onLoadMoreCurrencies,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.exchange_no_currencies_found),
            style = TariDesignSystem.typography.headingMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.exchange_no_currencies_description),
            style = TariDesignSystem.typography.body1,
            textAlign = TextAlign.Center,
            color = TariDesignSystem.colors.textSecondary,
        )
    }
}

@Composable
private fun LoadingItem(
    uiState: SelectCurrencyViewModel.UiState,
    onReloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TariLoadingLayout(
        modifier = modifier,
        targetLoadingState = when {
            uiState.loading -> TariLoadingLayoutState.Loading
            uiState.errorMessage != null -> TariLoadingLayoutState.Error
            else -> TariLoadingLayoutState.Content
        },
        loadingLayout = {
            TariProgressView(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
            )
        },
        errorLayout = {
            TariErrorView(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                onTryAgainClick = onReloadClick,
                errorMessage = uiState.errorMessage.orEmpty(),
            )
        },
        contentLayout = {
            Text(
                text = "You have loaded all currencies",
                style = TariDesignSystem.typography.body2,
                color = TariDesignSystem.colors.textSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )
        }
    )
}

@Preview
@Composable
private fun SelectCurrencyScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SelectCurrencyScreen(
            uiState = SelectCurrencyViewModel.UiState(
                currencies = MockDataStub.createCurrencyDtoList(5),
                selectedCurrency = MockDataStub.createCurrencyDtoList(5).first(),
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCurrencyItemClick = {},
            onLoadMoreCurrencies = {},
        )
    }
}

@Preview
@Composable
private fun SelectCurrencyScreenErrorPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SelectCurrencyScreen(
            uiState = SelectCurrencyViewModel.UiState(
                currencies = emptyList(),
                errorMessage = "An error occurred while loading currencies.",
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCurrencyItemClick = {},
            onLoadMoreCurrencies = {},
        )
    }
}

@Preview
@Composable
private fun SelectCurrencyScreenEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SelectCurrencyScreen(
            uiState = SelectCurrencyViewModel.UiState(
                currencies = emptyList(),
                totalCount = 0,
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCurrencyItemClick = {},
            onLoadMoreCurrencies = {},
        )
    }
}

@Preview
@Composable
private fun SelectCurrencyScreenProgressPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SelectCurrencyScreen(
            uiState = SelectCurrencyViewModel.UiState(
                currencies = emptyList(),
                loading = true,
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onCurrencyItemClick = {},
            onLoadMoreCurrencies = {},
        )
    }
}