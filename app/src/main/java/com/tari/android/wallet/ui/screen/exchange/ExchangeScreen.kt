package com.tari.android.wallet.ui.screen.exchange

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Clear
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
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.application.walletManager.formatAnyAmount
import com.tari.android.wallet.data.exolix.CurrencyDto
import com.tari.android.wallet.data.exolix.ExchangeDirection
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
import com.tari.android.wallet.ui.screen.exchange.widget.SelectedCurrencyChip
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.letNotNull
import com.tari.android.wallet.util.extension.newValueIfChanged
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun ExchangeScreen(
    uiState: ExchangeViewModel.UiState,
    onBackClick: () -> Unit,
    onReloadCurrencies: () -> Unit,
    onAmountChanged: (String) -> Unit,
    onSelectCurrencyClicked: () -> Unit,
    onMinAmountClicked: () -> Unit,
    onMaxAmountClicked: () -> Unit,
    onExchangeClicked: () -> Unit,
    onFixedRateToggled: (Boolean) -> Unit,
    onChangeDirectionClicked: () -> Unit,
    onPullToRefresh: () -> Unit,
    onRefreshClicked: () -> Unit,
    onDestinationAddressChanged: (String) -> Unit,
    onScanQrClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
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
        TariLoadingLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            targetLoadingState = when {
                uiState.loadingCurrencies -> TariLoadingLayoutState.Loading
                uiState.loadingCurrenciesError -> TariLoadingLayoutState.Error
                else -> TariLoadingLayoutState.Content
            },
            loadingLayout = {
                TariProgressView(Modifier.fillMaxSize())
            },
            errorLayout = {
                TariErrorView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    errorMessage = stringResource(R.string.exchange_currencies_load_error),
                    onTryAgainClick = onReloadCurrencies,
                )
            },
        ) {
            TariPullToRefreshBox(onPullToRefresh) {
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
                        uiState = uiState,
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
                        uiState = uiState,
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

                    if (uiState.exchangeDirection == ExchangeDirection.BUY_TARI) {
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
                        TariWalletAddressInfoBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        )
                    } else {
                        var textFieldValue by remember { mutableStateOf(TextFieldValue(uiState.destinationAddress)) }
                        textFieldValue = textFieldValue.newValueIfChanged(uiState.destinationAddress)
                        TariTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            value = textFieldValue,
                            onValueChanged = { newValue ->
                                onDestinationAddressChanged(newValue.text)
                                textFieldValue = newValue
                            },
                            hint = stringResource(R.string.exchange_destination_address_placeholder),
                            title = stringResource(R.string.exchange_destination_address_label, uiState.toCurrency?.coin.orEmpty()),
                            titleAdditionalLayout = {
                                IconButton(onClick = onScanQrClick) {
                                    Icon(
                                        painter = painterResource(R.drawable.vector_icon_qr),
                                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                                        contentDescription = stringResource(R.string.exchange_qr_scan_content_description),
                                    )
                                }
                            },
                            trailingIcon = if (uiState.destinationAddress.isNotBlank()) {
                                {
                                    IconButton(onClick = { onDestinationAddressChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Clear,
                                            tint = TariDesignSystem.colors.textSecondary,
                                            contentDescription = stringResource(R.string.exchange_clear_address_content_description),
                                        )
                                    }
                                }
                            } else null,
                            errorText = if (uiState.destinationAddressError) {
                                stringResource(R.string.exchange_invalid_address_error, uiState.selectedCurrency?.networkName.orEmpty())
                            } else null,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
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
                        text = stringResource(R.string.exchange_next_step_button),
                        enabled = uiState.exchangeButtonEnabled,
                        onClick = onExchangeClicked,
                    )
                    Spacer(Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun YouSendLayout(
    uiState: ExchangeViewModel.UiState,
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
            var textFieldValue by remember { mutableStateOf(TextFieldValue(uiState.amountValue)) }
            textFieldValue = textFieldValue.newValueIfChanged(uiState.amountValue)
            TariTextField(
                modifier = Modifier.weight(1f, false),
                value = textFieldValue,
                onValueChanged = { newValue ->
                    if (newValue.text != textFieldValue.text) onAmountChanged(newValue.text)
                    textFieldValue = newValue
                },
                hint = stringResource(R.string.exchange_amount_placeholder),
                errorText = when {
                    !uiState.rate?.message.isNullOrBlank() -> uiState.rate.message
                    uiState.amountErrorMessage != null -> stringResource(uiState.amountErrorMessage!!)
                    else -> null
                },
                visualTransformation = AmountVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            )
            Spacer(Modifier.size(16.dp))
            uiState.fromCurrency?.let { fromCurrency ->
                SelectedCurrencyChip(
                    modifier = Modifier.padding(top = 8.dp),
                    title = fromCurrency.coin,
                    subtitle = fromCurrency.networkName,
                    iconUrl = fromCurrency.iconUrl,
                    onClick = if (fromCurrency.selectable) onSelectCurrencyClicked else null,
                )
            }
        }

        if (uiState.exchangeDirection == ExchangeDirection.SELL_TARI) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(
                    R.string.home_available_to_spend_balance,
                    WalletConfig.balanceFormatter.format(uiState.availableBalance.tariValue) + " " + uiState.fromCurrency?.coin.orEmpty()
                ),
                style = TariDesignSystem.typography.body1,
                color = if (uiState.availableBalanceError) {
                    TariDesignSystem.colors.errorMain
                } else {
                    TariDesignSystem.colors.textSecondary
                },
            )
        }

        Spacer(Modifier.size(20.dp))
        val minAmountText = uiState.rate?.minAmount?.let { WalletConfig.amountFormatter.format(it) }
        val maxAmountText = uiState.rate?.maxAmount?.let { WalletConfig.amountFormatter.format(it) }
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = stringResource(R.string.exchange_min_amount_label_short),
                    style = TariDesignSystem.typography.body1,
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    modifier = Modifier.clickable(enabled = minAmountText != null) { onMinAmountClicked() },
                    text = "${minAmountText ?: "-"} ${uiState.fromCurrency?.coin.orEmpty()}",
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
                    modifier = Modifier.clickable(enabled = maxAmountText != null) { onMaxAmountClicked() },
                    text = "${maxAmountText ?: "-"} ${uiState.fromCurrency?.coin.orEmpty()}",
                    style = TariDesignSystem.typography.headingLarge,
                )
            }
        }
    }
}

@Composable
private fun YouReceiveLayout(
    uiState: ExchangeViewModel.UiState,
    onSelectCurrencyClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.exchange_you_receive_label),
            style = TariDesignSystem.typography.body1,
        )
        Spacer(Modifier.size(20.dp))

        Row {
            TariTextField(
                modifier = Modifier.weight(1f, false),
                value = TextFieldValue(uiState.rate?.toAmount?.toString() ?: ""),
                onValueChanged = {},
                hint = stringResource(R.string.exchange_converted_amount_placeholder),
                enabled = false,
                visualTransformation = AmountVisualTransformation(),
            )
            Spacer(Modifier.size(16.dp))
            uiState.toCurrency?.let { toCurrency ->
                SelectedCurrencyChip(
                    modifier = Modifier.padding(top = 8.dp),
                    title = toCurrency.coin,
                    subtitle = toCurrency.networkName,
                    iconUrl = toCurrency.iconUrl,
                    onClick = if (toCurrency.selectable) onSelectCurrencyClicked else null,
                )
            }
        }

        letNotNull(uiState.rate, uiState.fromCurrency, uiState.toCurrency) { rate, fromCurrency, toCurrency ->
            Spacer(Modifier.size(16.dp))
            Text(
                text = stringResource(
                    if (uiState.fixedRate) R.string.exchange_rate_display_fixed else R.string.exchange_rate_display_floating,
                    fromCurrency.coin,
                    rate.rate.formatAnyAmount(),
                    toCurrency.coin,
                ),
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
                availableBalance = 1000000.toMicroTari(),
                amountValue = "100",
                selectedCurrency = MockDataStub.createCurrencyDto(),
                tariCurrency = MockDataStub.createCurrencyDto(code = "XTM", name = "Tari"),
                rate = null,
                exchangeDirection = ExchangeDirection.BUY_TARI,
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenDarkPreview() {
    PreviewSecondarySurface(TariTheme.Dark) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                availableBalance = 1000000.toMicroTari(),
                amountValue = "100",
                selectedCurrency = MockDataStub.createCurrencyDto(),
                tariCurrency = MockDataStub.createCurrencyDto(code = "XTM", name = "Tari"),
                rate = MockDataStub.createRate(),
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenWrongAmountPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                availableBalance = 1000000.toMicroTari(),
                amountValue = "1000",
                rate = MockDataStub.createRate(
                    minAmount = 10.0.toBigDecimal(),
                    maxAmount = 500.0.toBigDecimal()
                ),
                selectedCurrency = MockDataStub.createCurrencyDto(),
                tariCurrency = MockDataStub.createCurrencyDto(code = "XTM", name = "Tari"),
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenCurrencyLoadingPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                availableBalance = 1000000.toMicroTari(),
                rateLoading = true,
                loadingCurrencies = true,
                loadingCurrenciesError = false,
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenCurrencyErrorPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                availableBalance = 1000000.toMicroTari(),
                loadingCurrencies = false,
                loadingCurrenciesError = true,
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenSellTariPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                availableBalance = 123_000000.toMicroTari(),
                amountValue = "100",
                selectedCurrency = CurrencyDto(
                    currency = MockDataStub.createCurrency(),
                    network = MockDataStub.createNetwork(),
                    selectable = true,
                ),
                tariCurrency = MockDataStub.createCurrencyDto(code = "XTM", name = "Tari"),
                rate = MockDataStub.createRate(),
                exchangeDirection = ExchangeDirection.SELL_TARI,
                destinationAddress = "0xe4a0b9f89c2dae9a25a44547e038e12e4fc56c31",
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeScreenSellTariInvalidAddressPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeScreen(
            uiState = ExchangeViewModel.UiState(
                availableBalance = 1000000.toMicroTari(),
                amountValue = "100",
                selectedCurrency = CurrencyDto(
                    currency = MockDataStub.createCurrency(),
                    network = MockDataStub.createNetwork(),
                    selectable = true,
                ),
                tariCurrency = MockDataStub.createCurrencyDto(code = "XTM", name = "Tari"),
                rate = MockDataStub.createRate(),
                exchangeDirection = ExchangeDirection.SELL_TARI,
                destinationAddress = "invalid_address_123",
            ),
            onBackClick = {},
            onReloadCurrencies = {},
            onAmountChanged = {},
            onSelectCurrencyClicked = {},
            onMinAmountClicked = {},
            onMaxAmountClicked = {},
            onExchangeClicked = {},
            onFixedRateToggled = {},
            onChangeDirectionClicked = {},
            onPullToRefresh = {},
            onRefreshClicked = {},
            onDestinationAddressChanged = {},
            onScanQrClick = {},
        )
    }
}