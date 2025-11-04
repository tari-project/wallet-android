package com.tari.android.wallet.ui.screen.home.exchange.exchangeStatus

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
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.home.exchange.widget.ExchangeStatusCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoCopyItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoItem
import com.tari.android.wallet.util.MockDataStub

@Composable
fun ExchangeStatusScreen(
    uiState: ExchangeStatusViewModel.UiState,
    onBackClick: () -> Unit,
    onTransactionDetailsClick: () -> Unit,
    onCopyClick: (String) -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.exchange_status_title),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TariProgressView()
                }
            },
            errorLayout = {
                TariErrorView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    errorMessage = uiState.error.orEmpty(),
                    onTryAgainClick = onRetry,
                )
            },
        ) {
            uiState.transaction?.let { transaction ->
                ExchangeStatusContent(
                    transaction = transaction,
                    onTransactionDetailsClick = onTransactionDetailsClick,
                    onCopyClick = onCopyClick,
                    onDoneClick = onBackClick,
                )
            }
        }
    }
}

@Composable
private fun ExchangeStatusContent(
    transaction: Exolix.TransactionResponse,
    onTransactionDetailsClick: () -> Unit,
    onCopyClick: (value: String) -> Unit,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.size(40.dp))

        ExchangeStatusCard(
            transaction = transaction,
        )

        Spacer(Modifier.size(32.dp))

        TxDetailInfoItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.exchange_status_amount_received),
            value = "${transaction.amountTo.toPlainString()} ${transaction.coinTo.coinCode}",
        )

        Spacer(Modifier.size(10.dp))

        TxDetailInfoCopyItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.exchange_status_destination_address, transaction.coinTo.networkName),
            value = transaction.withdrawalAddress,
            singleLine = false,
            onCopyClicked = onCopyClick,
        )

        Spacer(Modifier.size(10.dp))

        TxDetailInfoItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.exchange_status_transaction_details),
            value = transaction.id,
            singleLine = false,
        ) {
            androidx.compose.material3.IconButton(onClick = onTransactionDetailsClick) {
                Icon(
                    painter = painterResource(R.drawable.vector_icon_open_url),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarIcons,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(24.dp))

        TariPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.common_done),
            onClick = onDoneClick,
        )

        Spacer(Modifier.size(40.dp))
    }
}

@Composable
@Preview
private fun ExchangeStatusScreenSuccessPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusScreen(
            uiState = ExchangeStatusViewModel.UiState(
                transaction = MockDataStub.createTransactionResponse(),
            ),
            onBackClick = {},
            onTransactionDetailsClick = {},
            onCopyClick = {},
            onRetry = {},
        )
    }
}
