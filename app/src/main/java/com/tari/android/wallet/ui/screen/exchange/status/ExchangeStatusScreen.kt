package com.tari.android.wallet.ui.screen.exchange.status

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.application.walletManager.formatAnyAmount
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariPullToRefreshBox
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.exchange.widget.ExchangeStatusCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoCopyItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoItem
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.parseISODate
import com.tari.android.wallet.util.extension.txFormattedDate

@Composable
fun ExchangeStatusScreen(
    uiState: ExchangeStatusViewModel.UiState,
    onBackClick: () -> Unit,
    onCopyClick: (String) -> Unit,
    onRetry: () -> Unit,
    onPullToRefresh: () -> Unit,
    onShowQrCodeClick: () -> Unit,
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
                action = if (uiState.autoRefreshActive) {
                    {
                        Box(modifier = Modifier.size(48.dp)) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center),
                                    color = TariDesignSystem.colors.textPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                IconButton(
                                    modifier = Modifier.align(Alignment.Center),
                                    onClick = onPullToRefresh,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = stringResource(R.string.exchange_refresh_rate),
                                        tint = TariDesignSystem.colors.textPrimary,
                                    )
                                }
                            }
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
                uiState.loading -> TariLoadingLayoutState.Loading
                uiState.error != null -> TariLoadingLayoutState.Error
                else -> TariLoadingLayoutState.Content
            },
            loadingLayout = {
                TariProgressView(modifier = Modifier.fillMaxSize())
            },
            errorLayout = {
                TariErrorView(
                    modifier = Modifier.fillMaxSize(),
                    errorMessage = uiState.error.orEmpty(),
                    onTryAgainClick = onRetry,
                )
            },
        ) {
            uiState.transaction?.let { transaction ->
                TariPullToRefreshBox(onPullToRefresh) {
                    ExchangeStatusContent(
                        transaction = transaction,
                        showQrCodeButton = uiState.showQrCodeButton,
                        onCopyClick = onCopyClick,
                        onDoneClick = onBackClick,
                        onShowQrCodeClick = onShowQrCodeClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExchangeStatusContent(
    transaction: Exolix.Transaction,
    showQrCodeButton: Boolean,
    onCopyClick: (value: String) -> Unit,
    onDoneClick: () -> Unit,
    onShowQrCodeClick: () -> Unit,
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
            title = transaction.amountLabel(),
            value = "${WalletConfig.amountFormatter.format(transaction.amountTo)} ${transaction.coinTo.coinCode}",
        )

        Spacer(Modifier.size(10.dp))

        transaction.createdAt.parseISODate()?.let { date ->
            TxDetailInfoItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.exchange_status_created_at),
                value = date.txFormattedDate(),
            )
            Spacer(Modifier.size(10.dp))
        }

        TxDetailInfoItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.exchange_status_exchange_rate),
            value = stringResource(
                if (transaction.rateType == Exolix.RateType.FIXED) R.string.exchange_rate_value_fixed else R.string.exchange_rate_value_floating,
                transaction.coinFrom.coinCode,
                transaction.rate.formatAnyAmount(),
                transaction.coinTo.coinCode,
                when (transaction.rateType) {
                    Exolix.RateType.FIXED -> stringResource(R.string.exchange_status_rate_type_fixed)
                    Exolix.RateType.FLOATING -> stringResource(R.string.exchange_status_rate_type_floating)
                },
            ),
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

        if (!transaction.withdrawalExtraId.isNullOrBlank()) {
            TxDetailInfoCopyItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.exchange_status_destination_extra_id),
                value = transaction.withdrawalExtraId,
                singleLine = false,
                onCopyClicked = onCopyClick,
            )
            Spacer(Modifier.size(10.dp))
        }

        if (!transaction.comment.isNullOrBlank()) {
            TxDetailInfoItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.exchange_status_comment),
                value = transaction.comment,
                singleLine = false,
            )
            Spacer(Modifier.size(10.dp))
        }

        if (!transaction.refundAddress.isNullOrBlank()) {
            TxDetailInfoCopyItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.exchange_status_refund_address, transaction.coinFrom.networkName),
                value = transaction.refundAddress,
                singleLine = false,
                onCopyClicked = onCopyClick,
            )
            Spacer(Modifier.size(10.dp))
        }

        if (!transaction.refundExtraId.isNullOrBlank()) {
            TxDetailInfoCopyItem(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(R.string.exchange_status_refund_extra_id),
                value = transaction.refundExtraId,
                singleLine = false,
                onCopyClicked = onCopyClick,
            )
            Spacer(Modifier.size(10.dp))
        }

        TxDetailInfoCopyItem(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.exchange_status_transaction_details),
            value = transaction.id,
            singleLine = false,
        )

        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(24.dp))

        if (showQrCodeButton) {
            TariPrimaryButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.exchange_status_show_qr_code),
                onClick = onShowQrCodeClick,
            )
            Spacer(Modifier.size(16.dp))
        }

        TariPrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.common_done),
            onClick = onDoneClick,
        )

        Spacer(Modifier.size(40.dp))
    }
}

@Composable
private fun Exolix.Transaction.amountLabel(): String =
    when (this.status) {
        Exolix.TransactionStatus.WAIT,
        Exolix.TransactionStatus.CONFIRMATION,
        Exolix.TransactionStatus.CONFIRMED,
        Exolix.TransactionStatus.EXCHANGING,
        Exolix.TransactionStatus.SENDING -> stringResource(R.string.exchange_status_amount_to_receive)

        Exolix.TransactionStatus.SUCCESS -> stringResource(R.string.exchange_status_amount_received)
        Exolix.TransactionStatus.OVERDUE,
        Exolix.TransactionStatus.REFUNDED -> stringResource(R.string.exchange_status_amount)
    }

@Composable
@Preview
private fun ExchangeStatusScreenSuccessPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusScreen(
            uiState = ExchangeStatusViewModel.UiState(
                transaction = MockDataStub.createExchangeTransaction(
                    id = "dummy_id",
                    amountTo = java.math.BigDecimal("123.45"),
                    rate = java.math.BigDecimal("0.005"),
                    rateType = Exolix.RateType.FIXED,
                    createdAt = "2024-06-01T12:00:00Z",
                    withdrawalAddress = "0x9876543210fedcba9876543210fedcba98765432",
                    withdrawalExtraId = "withdrawal_extra_id",
                    refundAddress = "0x1234567890abcdef1234567890abcdef12345678",
                    refundExtraId = "dummy_refund_extra_id",
                    comment = "Test exchange transaction",
                ),
                autoRefreshActive = true,
            ),
            onBackClick = {},
            onCopyClick = {},
            onRetry = {},
            onPullToRefresh = {},
            onShowQrCodeClick = {},
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusScreenWaitPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusScreen(
            uiState = ExchangeStatusViewModel.UiState(
                transaction = MockDataStub.createExchangeTransaction(
                    id = "dummy_id",
                    status = Exolix.TransactionStatus.WAIT,
                    amountTo = java.math.BigDecimal("123.45"),
                    rate = java.math.BigDecimal("0.005"),
                    rateType = Exolix.RateType.FIXED,
                    createdAt = "2024-06-01T12:00:00Z",
                    withdrawalAddress = "0x9876543210fedcba9876543210fedcba98765432",
                ),
                autoRefreshActive = true,
            ),
            onBackClick = {},
            onCopyClick = {},
            onRetry = {},
            onPullToRefresh = {},
            onShowQrCodeClick = {},
        )
    }
}
