package com.tari.android.wallet.ui.screen.home.exchange

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.AmountVisualTransformation
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTextField
import com.tari.android.wallet.ui.screen.home.exchange.widget.CurrencyItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.newValueIfChanged

@Composable
fun ExchangeScreen(
    uiState: ExchangeViewModel.UiState,
    onReloadDefCurrency: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onSelectCurrencyClicked: () -> Unit,
    onSelectNetworkClicked: () -> Unit,
    onMinAmountClicked: () -> Unit,
    onMaxAmountClicked: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.size(32.dp))
        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.exchange_title),
            style = TariDesignSystem.typography.headingXLarge,
        )
        Spacer(Modifier.size(32.dp))

        CurrencyItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            title = uiState.fromCurrency.name,
            subtitle = uiState.fromCurrency.code,
            iconUrl = uiState.fromCurrency.icon,
        )
        Spacer(Modifier.size(16.dp))

        var amountValue by remember { mutableStateOf(TextFieldValue(uiState.amount.toString())) }
        amountValue = amountValue.newValueIfChanged(uiState.amountValue)
        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = amountValue,
            onValueChanged = { newValue ->
                if (newValue.text != amountValue.text) onAmountChanged(newValue.text)
                amountValue = newValue
            },
            hint = stringResource(R.string.exchange_amount_placeholder),
            errorText = when {
                !uiState.rate?.message.isNullOrBlank() -> uiState.rate.message
                uiState.rateError -> stringResource(R.string.exchange_invalid_amount_error)
                else -> null
            },
            visualTransformation = AmountVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )

        if (uiState.rateError && uiState.rate != null) {
            Row(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.exchange_min_amount_label_short),
                    style = TariDesignSystem.typography.body1,
                    color = TariDesignSystem.colors.textSecondary,
                )
                Text(
                    modifier = Modifier.clickable { onMinAmountClicked() },
                    text = uiState.rate.minAmount.toString(),
                    style = TariDesignSystem.typography.headingLarge,
                    color = TariDesignSystem.colors.secondaryMain,
                )
                Text(
                    text = stringResource(R.string.exchange_max_amount_label_short),
                    style = TariDesignSystem.typography.body1,
                    color = TariDesignSystem.colors.textSecondary,
                )
                Text(
                    modifier = Modifier.clickable { onMaxAmountClicked() },
                    text = uiState.rate.maxAmount.toString(),
                    style = TariDesignSystem.typography.headingLarge,
                    color = TariDesignSystem.colors.secondaryMain,
                )
            }
        }

        TariLoadingLayout(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(8.dp),
            targetLoadingState = if (uiState.rateLoading) TariLoadingLayoutState.Loading else TariLoadingLayoutState.Content,
        ) {
            Image(
                painter = painterResource(R.drawable.vector_tx_detail_arrow_down),
                contentDescription = null,
            )
        }

        TariTextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            value = TextFieldValue(uiState.rate?.toAmount?.toString() ?: ""),
            onValueChanged = {}, // Disabled
            hint = stringResource(R.string.exchange_converted_amount_placeholder),
            enabled = false,
            visualTransformation = AmountVisualTransformation(),
        )

        TariLoadingLayout(
            targetLoadingState = when {
                uiState.loadingDefaultCurrency -> TariLoadingLayoutState.Loading
                uiState.loadingDefaultCurrencyError -> TariLoadingLayoutState.Error
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
                        .fillMaxWidth()
                        .padding(32.dp),
                    onTryAgainClick = onReloadDefCurrency,
                )
            },
        ) {
            Spacer(Modifier.size(16.dp))
            uiState.selectedToCurrency?.let { toCurrency ->
                CurrencyItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    title = toCurrency.name,
                    subtitle = toCurrency.code,
                    iconUrl = toCurrency.icon,
                    showArrow = true,
                    onClick = onSelectCurrencyClicked,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.exchange_network_label),
                    style = TariDesignSystem.typography.body2
                )
                Spacer(Modifier.size(4.dp))
                uiState.selectedToNetwork?.let { network ->
                    CurrencyItem(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        title = network.name,
                        subtitle = network.shortName.orEmpty(),
                        iconUrl = network.icon,
                        showArrow = uiState.selectedToCurrency.supportsMultipleNetworks,
                        onClick = onSelectNetworkClicked.takeIf { uiState.selectedToCurrency.supportsMultipleNetworks },
                    )
                }

                uiState.rate?.let { rate ->
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = stringResource(R.string.exchange_rate_display, uiState.fromCurrency.code, rate.rate.toString(), toCurrency.code),
                        style = TariDesignSystem.typography.headingMedium,
                        color = TariDesignSystem.colors.textSecondary,
                    )
                }

                Spacer(Modifier.size(16.dp))
            }
        }
    }
}

@Composable
@Preview
private fun ExchangeScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                amountValue = "100",
                selectedToCurrency = MockDataStub.createCurrency(),
                selectedToNetwork = MockDataStub.createNetwork(),
                rate = MockDataStub.createRate(
                    toAmount = 123.21321f.toBigDecimal()
                ),
                exchangeInfo = "ID: 12345, Amount: 100, Status: pending",
            ),
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onSelectNetworkClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenWrongAmountPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                amountValue = "1000",
                rate = MockDataStub.createRate(
                    minAmount = 10.0.toBigDecimal(),
                    maxAmount = 500.0.toBigDecimal()
                ),
                selectedToCurrency = MockDataStub.createCurrency(),
                selectedToNetwork = MockDataStub.createNetwork(),
            ),
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onSelectNetworkClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenCurrencyLoadingPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                rateLoading = true,
                loadingDefaultCurrency = true,
                loadingDefaultCurrencyError = false,
            ),
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onSelectNetworkClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenCurrencyErrorPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                loadingDefaultCurrency = false,
                loadingDefaultCurrencyError = true,
            ),
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onSelectNetworkClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
        )
    }
}