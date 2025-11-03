package com.tari.android.wallet.ui.screen.home.exchange

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.data.exolix.ExchangeDirection
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.AmountVisualTransformation
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariPullToRefreshBox
import com.tari.android.wallet.ui.compose.components.TariSwitch
import com.tari.android.wallet.ui.compose.components.TariTextField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.home.exchange.widget.SelectedCurrencyChip
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.newValueIfChanged

@Composable
fun ExchangeScreen(
    uiState: ExchangeViewModel.UiState,
    onBackClick: () -> Unit,
    onReloadDefCurrency: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onSelectCurrencyClicked: () -> Unit,
    onMinAmountClicked: () -> Unit,
    onMaxAmountClicked: () -> Unit,
    onExchangeClicked: () -> Unit,
    onFixedRateToggled: (Boolean) -> Unit,
    onChangeDirectionClicked: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRefreshClicked: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.exchange_title),
                onBack = onBackClick,
                action = if (uiState.autoRefreshActive) {
                    {
                        IconButton(onClick = onRefreshClicked) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.exchange_refresh_rate),
                                tint = TariDesignSystem.colors.textPrimary,
                            )
                        }
                    }
                } else null,
            )
        },
    ) { paddingValues ->
        TariPullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            onPullToRefresh = onPullToRefresh,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(Modifier.size(20.dp))

                YouSendLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    fromCurrency = uiState.fromCurrency,
                    rate = uiState.rate,
                    amountError = uiState.amountError,
                    amountValue = uiState.amountValue,
                    onAmountChanged = onAmountChanged,
                    onMinAmountClicked = onMinAmountClicked,
                    onMaxAmountClicked = onMaxAmountClicked,
                    onSelectCurrencyClicked = onSelectCurrencyClicked,
                )

                Spacer(Modifier.size(20.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TariHorizontalDivider(Modifier.weight(1f))
                    TariLoadingLayout(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .size(48.dp),
                        targetLoadingState = if (uiState.rateLoading) TariLoadingLayoutState.Loading else TariLoadingLayoutState.Content,
                        loadingLayout = {
                            Box(modifier = Modifier.fillMaxSize()) { TariProgressView(modifier = Modifier.align(Alignment.Center)) }
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            IconButton(
                                modifier = Modifier.align(Alignment.Center),
                                onClick = onChangeDirectionClicked,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.vector_two_arrows_circle),
                                    tint = TariDesignSystem.colors.textPrimary,
                                    contentDescription = null,
                                )
                            }
                        }
                    }
                    TariHorizontalDivider(Modifier.weight(1f))
                }
                Spacer(Modifier.size(20.dp))

                YouReceiveLayout(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    fromCurrency = uiState.fromCurrency,
                    toCurrency = uiState.toCurrency,
                    rate = uiState.rate,
                    loadingDefaultCurrency = uiState.loadingDefaultCurrency,
                    loadingDefaultCurrencyError = uiState.loadingDefaultCurrencyError,
                    onReloadDefaultCurrency = onReloadDefCurrency,
                    onSelectCurrencyClicked = onSelectCurrencyClicked,
                )
                Spacer(Modifier.size(20.dp))

                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TariSwitch(
                        checked = uiState.fixedRate,
                        onCheckedChange = onFixedRateToggled,
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.exchange_fixed_rate),
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.textPrimary,
                    )
                }

                Spacer(Modifier.size(20.dp))
                uiState.toCurrency?.let { toCurrency ->
                    Text(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        text = stringResource(R.string.exchange_destination_address_label, toCurrency.coin),
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.textPrimary,
                    )
                }
                Spacer(Modifier.size(12.dp))

                if (uiState.exchangeDirection == ExchangeDirection.BUY_TARI) {
                    TariWalletAddressInfoBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }

                Spacer(Modifier.weight(1f))

                Spacer(Modifier.size(20.dp))
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = stringResource(R.string.exchange_powered_by_label),
                    style = TariDesignSystem.typography.body1,
                )
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 92.dp, height = 32.dp),
                    contentScale = ContentScale.FillWidth,
                    painter = painterResource(R.drawable.exolix_logo),
                    contentDescription = null,
                )
                Spacer(Modifier.size(40.dp))

                TariPrimaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    text = stringResource(R.string.exchange_exchange_now_button),
                    enabled = uiState.exchangeButtonEnabled,
                    onClick = onExchangeClicked,
                )
                Spacer(Modifier.size(40.dp))
            }
        }
    }
}

@Composable
private fun YouSendLayout(
    fromCurrency: CurrencyDto?,
    rate: Exolix.Rate?,
    amountError: Boolean,
    amountValue: String,
    onAmountChanged: (String) -> Unit,
    onMinAmountClicked: () -> Unit,
    onMaxAmountClicked: () -> Unit,
    onSelectCurrencyClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Column(modifier = modifier) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.exchange_you_send_label),
            style = TariDesignSystem.typography.body1,
        )
        Spacer(Modifier.size(20.dp))
        Row {
            var textFieldValue by remember { mutableStateOf(TextFieldValue(amountValue)) }
            textFieldValue = textFieldValue.newValueIfChanged(amountValue)
            TariTextField(
                modifier = Modifier.weight(1f, false),
                value = textFieldValue,
                onValueChanged = { newValue ->
                    if (newValue.text != textFieldValue.text) onAmountChanged(newValue.text)
                    textFieldValue = newValue
                },
                hint = stringResource(R.string.exchange_amount_placeholder),
                errorText = when {
                    !rate?.message.isNullOrBlank() -> rate.message
                    amountError -> stringResource(R.string.exchange_invalid_amount_error)
                    else -> null
                },
                visualTransformation = AmountVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            Spacer(Modifier.size(16.dp))
            if (fromCurrency != null) {
                SelectedCurrencyChip(
                    modifier = Modifier.padding(top = 8.dp),
                    title = fromCurrency.coin,
                    subtitle = fromCurrency.networkName,
                    iconUrl = fromCurrency.iconUrl,
                    onClick = if (fromCurrency.selectable) onSelectCurrencyClicked else null,
                )
            }
        }

        if (amountError && rate != null) {
            Spacer(Modifier.size(20.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.exchange_min_amount_label_short),
                        style = TariDesignSystem.typography.body1,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        modifier = Modifier.clickable { onMinAmountClicked() },
                        text = "${rate.minAmount} ${fromCurrency?.coin.orEmpty()}",
                        style = TariDesignSystem.typography.headingLarge,
                    )
                }
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.exchange_max_amount_label_short),
                        style = TariDesignSystem.typography.body1,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        modifier = Modifier.clickable { onMaxAmountClicked() },
                        text = "${rate.maxAmount} ${fromCurrency?.coin.orEmpty()}",
                        style = TariDesignSystem.typography.headingLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun YouReceiveLayout(
    fromCurrency: CurrencyDto?,
    toCurrency: CurrencyDto?,
    rate: Exolix.Rate?,
    loadingDefaultCurrency: Boolean,
    loadingDefaultCurrencyError: Boolean,
    onReloadDefaultCurrency: () -> Unit,
    onSelectCurrencyClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TariLoadingLayout(
        modifier = modifier,
        targetLoadingState = when {
            loadingDefaultCurrency -> TariLoadingLayoutState.Loading
            loadingDefaultCurrencyError -> TariLoadingLayoutState.Error
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
                onTryAgainClick = onReloadDefaultCurrency,
            )
        },
    ) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.exchange_you_receive_label),
            style = TariDesignSystem.typography.body1,
        )
        Spacer(Modifier.size(20.dp))

        Row {
            TariTextField(
                modifier = Modifier.weight(1f, false),
                value = TextFieldValue(rate?.toAmount?.toString() ?: ""),
                onValueChanged = {},
                hint = stringResource(R.string.exchange_converted_amount_placeholder),
                enabled = false,
                visualTransformation = AmountVisualTransformation(),
            )
            Spacer(Modifier.size(16.dp))
            if (toCurrency != null) {
                SelectedCurrencyChip(
                    modifier = Modifier.padding(top = 8.dp),
                    title = toCurrency.coin,
                    subtitle = toCurrency.networkName,
                    iconUrl = toCurrency.iconUrl,
                    onClick = if (toCurrency.selectable) onSelectCurrencyClicked else null,
                )
            }
        }

        if (rate != null && fromCurrency != null && toCurrency != null) {
            Spacer(Modifier.size(16.dp))
            Text(
                text = stringResource(R.string.exchange_rate_display, fromCurrency.coin, rate.rate.toString(), toCurrency.coin),
                style = TariDesignSystem.typography.headingMedium,
            )
        }
    }
}

@Composable
private fun TariWalletAddressInfoBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(TariDesignSystem.shapes.chip)
            .border(
                border = BorderStroke(1.dp, TariDesignSystem.colors.secondaryMain),
                shape = TariDesignSystem.shapes.chip,
            )
            .background(TariDesignSystem.colors.backgroundAccent)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.exchange_tari_wallet_address_info),
            style = TariDesignSystem.typography.body1,
            color = TariDesignSystem.colors.textPrimary,
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                amountValue = "100",
                selectedCurrency = MockDataStub.createCurrencyDto(),
                rate = MockDataStub.createRate(),
                exchangeDirection = ExchangeDirection.BUY_TARI,
            ),
            onBackClick = {},
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenDarkPreview() {
    PreviewSecondarySurface(TariTheme.Dark) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                amountValue = "100",
                selectedCurrency = MockDataStub.createCurrencyDto(),
                rate = MockDataStub.createRate(),
            ),
            onBackClick = {},
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
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
                selectedCurrency = MockDataStub.createCurrencyDto(),
            ),
            onBackClick = {},
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
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
            onBackClick = {},
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
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
            onBackClick = {},
            onReloadDefCurrency = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
        )
    }
}