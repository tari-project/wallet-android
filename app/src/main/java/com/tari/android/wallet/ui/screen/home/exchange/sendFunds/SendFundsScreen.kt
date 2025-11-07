package com.tari.android.wallet.ui.screen.home.exchange.sendFunds

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.ExchangeDirection
import com.tari.android.wallet.data.exolix.ExchangeRequestData
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.compose.widgets.QrCodeCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import java.math.BigDecimal

@Composable
fun SendFundsScreen(
    uiState: SendFundsViewModel.UiState,
    onBackClick: () -> Unit,
    onCopyAmount: () -> Unit,
    onCopyAddress: () -> Unit,
    onCancelTransaction: () -> Unit,
    onOpenTxDetails: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.exchange_send_funds_title),
                onBack = onBackClick,
            )
        },
    ) { paddingValues ->
        TariLoadingLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            targetLoadingState = when {
                uiState.loading -> TariLoadingLayoutState.Loading
                uiState.error != null -> TariLoadingLayoutState.Error
                else -> TariLoadingLayoutState.Content
            },
            loadingLayout = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    TariProgressView()
                }
            },
            errorLayout = {
                TariErrorView(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    errorMessage = uiState.error.orEmpty(),
                    onTryAgainClick = onRetry,
                )
            },
        ) {
            if (uiState.depositAddress != null) {
                SendFundsContent(
                    request = uiState.request,
                    depositAddress = uiState.depositAddress,
                    qrBitmap = uiState.qrBitmap,
                    onCopyAmount = onCopyAmount,
                    onCopyAddress = onCopyAddress,
                    onCancelTransaction = onCancelTransaction,
                    onOpenTxDetails = onOpenTxDetails,
                )
            }
        }
    }
}

@Composable
private fun SendFundsContent(
    request: ExchangeRequestData,
    depositAddress: String,
    qrBitmap: Bitmap?,
    onCopyAmount: () -> Unit,
    onCopyAddress: () -> Unit,
    onCancelTransaction: () -> Unit,
    onOpenTxDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Spacer(Modifier.size(40.dp))

        Text(
            modifier = modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.exchange_send_exact_amount),
            style = TariDesignSystem.typography.headingLarge,
            color = TariDesignSystem.colors.textPrimary,
        )

        Spacer(Modifier.size(20.dp))

        Card(
            modifier = modifier.padding(horizontal = 16.dp),
            shape = TariDesignSystem.shapes.card,
            colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.exchange_you_need_to_send),
                    style = TariDesignSystem.typography.body1,
                    color = TariDesignSystem.colors.textSecondary,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${request.amount} ${request.selectedCurrency.coin}",
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.textPrimary,
                    )
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onCopyAmount,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.vector_icon_copy),
                            contentDescription = stringResource(R.string.exchange_copy_amount_content_description),
                            tint = TariDesignSystem.colors.secondaryMain,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = request.selectedCurrency.networkName,
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.textSecondary,
                    )
                }

                Spacer(Modifier.size(8.dp))

                Text(
                    text = stringResource(R.string.exchange_to_exolix_address),
                    style = TariDesignSystem.typography.body1,
                    color = TariDesignSystem.colors.textSecondary,
                )
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = depositAddress,
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.textPrimary,
                    )
                    IconButton(
                        modifier = Modifier.size(32.dp),
                        onClick = onCopyAddress,
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = painterResource(R.drawable.vector_icon_copy),
                            contentDescription = stringResource(R.string.exchange_copy_address_content_description),
                            tint = TariDesignSystem.colors.secondaryMain,
                        )
                    }
                }

                Spacer(Modifier.size(16.dp))

                QrCodeCard(
                    qrBitmap = qrBitmap,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(Modifier.size(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(TariDesignSystem.shapes.card)
                        .background(TariDesignSystem.colors.systemSecondaryGreen)
                        .border(1.dp, TariDesignSystem.colors.successDark, TariDesignSystem.shapes.card),
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        text = stringResource(R.string.exchange_send_funds_once),
                        style = TariDesignSystem.typography.headingSmall,
                        color = TariDesignSystem.colors.successDark,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(40.dp))

        TariPrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            text = "OPEN TX DETAILS (TODO)",
            onClick = onOpenTxDetails,
        )

        TariTextButton(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.CenterHorizontally),
            text = stringResource(R.string.exchange_cancel_transaction),
            warningColor = true,
            onClick = onCancelTransaction,
        )
        Spacer(Modifier.size(40.dp))
    }
}

@Composable
@Preview
private fun SendFundsScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SendFundsScreen(
            uiState = SendFundsViewModel.UiState(
                request = ExchangeRequestData(
                    selectedCurrency = MockDataStub.createCurrencyDto(
                        code = "ETH",
                        name = "Ethereum",
                    ),
                    tariCurrency = MockDataStub.createCurrencyDto(code = "XTM", name = "Tari"),
                    selectedAddress = null,
                    amount = BigDecimal("0.124"),
                    rate = MockDataStub.createRate(),
                    direction = ExchangeDirection.BUY_TARI,
                ),
                exchangeId = "test-exchange-id-12345",
                depositAddress = "0x311c25b376bd55608bf836424ac0b7616e044c920x311c25b376bd55608bf836424ac0b7616e044c92",
                qrBitmap = null,
                loading = false,
            ),
            onBackClick = {},
            onCopyAmount = {},
            onCopyAddress = {},
            onCancelTransaction = {},
            onRetry = {},
            onOpenTxDetails = {},
        )
    }
}
