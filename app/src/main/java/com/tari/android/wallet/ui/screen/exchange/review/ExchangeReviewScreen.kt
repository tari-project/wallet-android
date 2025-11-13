package com.tari.android.wallet.ui.screen.exchange.review

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.exolix.ExchangeDirection
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.exchange.widget.SelectedCurrencyChip
import com.tari.android.wallet.ui.screen.send.confirm.widget.SenderCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoAddressItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoCopyItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoItem
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun ExchangeReviewScreen(
    uiState: ExchangeReviewViewModel.UiState,
    onBackClick: () -> Unit,
    onCopyValueClick: (value: String) -> Unit,
    onConfirmClick: () -> Unit,
    onRetry: () -> Unit,
    onEmojiIdDetailsClick: () -> Unit,
    onFeeInfoClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.exchange_review_title),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        TariLoadingLayout(
            modifier = Modifier.padding(paddingValues),
            targetLoadingState = when {
                uiState.creatingExchange -> TariLoadingLayoutState.Loading
                uiState.creatingExchangeError != null -> TariLoadingLayoutState.Error
                else -> TariLoadingLayoutState.Content
            },
            loadingLayout = {
                TariProgressView(Modifier.fillMaxSize())
            },
            errorLayout = {
                TariErrorView(
                    modifier = Modifier.fillMaxSize(),
                    errorMessage = uiState.creatingExchangeError.orEmpty(),
                    onTryAgainClick = onRetry,
                )
            },
        ) {
            ReviewContent(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                onCopyValueClick = onCopyValueClick,
                onConfirmClick = onConfirmClick,
                onEmojiIdDetailsClick = onEmojiIdDetailsClick,
                onFeeInfoClick = onFeeInfoClick,
            )
        }
    }
}

@Composable
private fun ReviewContent(
    uiState: ExchangeReviewViewModel.UiState,
    onCopyValueClick: (value: String) -> Unit,
    onConfirmClick: () -> Unit,
    onEmojiIdDetailsClick: () -> Unit,
    onFeeInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.size(36.dp))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            text = stringResource(R.string.exchange_review_you_are_about_to_swap),
            style = TariDesignSystem.typography.headingLarge,
        )
        Spacer(Modifier.size(16.dp))

        Box {
            Column {
                SenderCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    title = "${WalletConfig.amountFormatter.format(uiState.request.amount)} ${uiState.request.tariCurrency.coin}",
                    endIcon = {
                        SelectedCurrencyChip(
                            title = uiState.request.tariCurrency.coin,
                            subtitle = uiState.request.tariCurrency.networkName,
                            iconUrl = uiState.request.tariCurrency.iconUrl,
                            onClick = null,
                        )
                    }
                )
                Spacer(Modifier.size(8.dp))
                SenderCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    title = "${WalletConfig.amountFormatter.format(uiState.request.rate.toAmount)} ${uiState.request.selectedCurrency.coin}",
                    endIcon = {
                        SelectedCurrencyChip(
                            title = uiState.request.selectedCurrency.coin,
                            subtitle = uiState.request.selectedCurrency.networkName,
                            iconUrl = uiState.request.selectedCurrency.iconUrl,
                            onClick = null,
                        )
                    }
                )
            }
            Image(
                modifier = Modifier.align(Alignment.Center),
                painter = painterResource(R.drawable.vector_tx_detail_arrow_down),
                contentDescription = null,
            )
        }

        Spacer(Modifier.size(24.dp))

        uiState.transaction?.let { transaction ->
            TxDetailInfoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                title = stringResource(R.string.exchange_status_exchange_fee),
                value = "${WalletConfig.amountFormatter.format(uiState.fee.tariValue) ?: "-"} ${transaction.coinFrom.coinCode}",
            ) {
                IconButton(onClick = onFeeInfoClick) {
                    Icon(
                        painter = painterResource(R.drawable.vector_icon_question_circle),
                        contentDescription = null,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                    )
                }
            }

            Spacer(Modifier.size(10.dp))

            TxDetailInfoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                title = stringResource(R.string.exchange_status_exchange_rate),
                value = stringResource(
                    if (transaction.rateType == Exolix.RateType.FIXED) R.string.exchange_rate_value_fixed else R.string.exchange_rate_value_floating,
                    transaction.coinFrom.coinCode,
                    WalletConfig.formatAnyAmount(transaction.rate),
                    transaction.coinTo.coinCode,
                    when (transaction.rateType) {
                        Exolix.RateType.FIXED -> stringResource(R.string.exchange_status_rate_type_fixed)
                        Exolix.RateType.FLOATING -> stringResource(R.string.exchange_status_rate_type_floating)
                    },
                ),
            )
        }

        Spacer(Modifier.size(10.dp))

        TxDetailInfoAddressItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            title = stringResource(R.string.exchange_review_from_wallet),
            walletAddress = uiState.walletAddress,
            onCopyClicked = onCopyValueClick,
            onEmojiIdDetailsClick = onEmojiIdDetailsClick,
        )

        Spacer(Modifier.size(10.dp))

        uiState.transaction?.let { transaction ->
            TxDetailInfoCopyItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                title = stringResource(R.string.exchange_review_to_exolix_address),
                value = transaction.depositAddress,
                singleLine = false,
                onCopyClicked = onCopyValueClick,
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

        Spacer(Modifier.size(24.dp))

        TariLoadingLayout(
            modifier = Modifier.fillMaxWidth(),
            targetLoadingState = if (uiState.sending) TariLoadingLayoutState.Loading else TariLoadingLayoutState.Content,
            loadingLayout = {
                TariProgressView(Modifier.fillMaxWidth())
            },
        ) {
            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.exchange_review_confirm_button),
                onClick = onConfirmClick,
            )
        }

        Spacer(Modifier.size(40.dp))
    }
}

@Composable
@Preview
private fun ExchangeReviewScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeReviewScreen(
            uiState = ExchangeReviewViewModel.UiState(
                request = MockDataStub.createExchangeRequestData(
                    direction = ExchangeDirection.SELL_TARI,
                ),
                fee = 1000.toMicroTari(),
                walletAddress = MockDataStub.createWalletAddress(),
                transaction = MockDataStub.createExchangeTransaction(),
                creatingExchange = false,
                creatingExchangeError = null,
            ),
            onBackClick = {},
            onConfirmClick = {},
            onCopyValueClick = {},
            onRetry = {},
            onEmojiIdDetailsClick = {},
            onFeeInfoClick = {},
        )
    }
}
