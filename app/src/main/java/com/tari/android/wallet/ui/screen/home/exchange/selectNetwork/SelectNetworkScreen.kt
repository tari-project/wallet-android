package com.tari.android.wallet.ui.screen.home.exchange.selectNetwork

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariSearchField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.home.exchange.widget.CurrencyItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@Composable
fun SelectNetworkScreen(
    uiState: SelectNetworkViewModel.UiState,
    onBackClick: () -> Unit,
    onSearchQueryChange: (query: String) -> Unit,
    onNetworkItemClick: (network: Exolix.Network) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.exchange_select_network_title),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        var searchQuery by remember { mutableStateOf(TextFieldValue(uiState.searchQuery)) }

        if (uiState.showEmptyState) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                item {
                    Spacer(Modifier.size(20.dp))
                    TariSearchField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        searchQuery = searchQuery,
                        hint = stringResource(R.string.exchange_search_network_hint),
                        onQueryChanged = {
                            searchQuery = it
                            onSearchQueryChange(it.text)
                        },
                    )
                    Spacer(Modifier.size(20.dp))
                }

                items(uiState.filteredNetworks) { network ->
                    CurrencyItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                        title = network.name,
                        subtitle = network.shortName.orEmpty(),
                        iconUrl = network.icon,
                        defaut = network.isDefault,
                        selected = network == uiState.selectedNetwork,
                        onClick = { onNetworkItemClick(network) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.exchange_no_networks_found),
            style = TariDesignSystem.typography.headingMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = stringResource(R.string.exchange_no_networks_description),
            style = TariDesignSystem.typography.body1,
            textAlign = TextAlign.Center,
            color = TariDesignSystem.colors.textSecondary,
        )
    }
}

@Preview
@Composable
private fun SelectNetworkScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SelectNetworkScreen(
            uiState = SelectNetworkViewModel.UiState(
                networks = MockDataStub.createNetworkList(),
                selectedNetwork = MockDataStub.createNetwork(),
            ),
            onBackClick = {},
            onSearchQueryChange = {},
            onNetworkItemClick = {},
        )
    }
}