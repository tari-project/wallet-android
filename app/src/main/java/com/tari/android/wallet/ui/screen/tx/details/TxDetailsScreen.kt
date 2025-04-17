package com.tari.android.wallet.ui.screen.tx.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.compose.widgets.DotsAnimation
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoAddressItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoContactNameItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoCopyItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoStatusItem
import com.tari.android.wallet.util.MockDataStub

@Composable
fun TxDetailsScreen(
    uiState: TxDetailsModel.UiState,
    onBackClick: () -> Unit,
    onCancelTxClick: () -> Unit,
    onCopyValueClick: (value: String) -> Unit,
    onBlockExplorerClick: () -> Unit,
    onContactEditClick: () -> Unit,
    onFeeInfoClick: () -> Unit,
    onEmojiIdDetailsClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(uiState.screenTitle),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.size(24.dp))

            TxDetailInfoCopyItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.tx_details_paid),
                value = "${WalletConfig.amountFormatter.format(uiState.tx.amount.tariValue)} ${uiState.ticker}",
                onCopyClicked = onCopyValueClick,
            )

            uiState.contact?.walletAddress?.let { walletAddress ->
                Spacer(Modifier.size(10.dp))
                TxDetailInfoAddressItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    walletAddress = walletAddress,
                    onCopyClicked = onCopyValueClick,
                    onEmojiIdDetailsClick = onEmojiIdDetailsClick,
                )
            }

            uiState.contact?.let { contact ->
                Spacer(Modifier.size(10.dp))
                TxDetailInfoContactNameItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    alias = contact.alias,
                    onEditClicked = onContactEditClick,
                )
            }

            uiState.txFee?.let { fee ->
                val feeValue = "${WalletConfig.amountFormatter.format(fee.tariValue)} ${uiState.ticker}"
                Spacer(Modifier.size(10.dp))
                TxDetailInfoItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.tx_detail_transaction_fee),
                    value = feeValue,
                ) {
                    IconButton(
                        modifier = Modifier.offset(x = 12.dp), // needed to remove "margin" between two IconButtons
                        onClick = onFeeInfoClick,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_question_circle),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }

                    IconButton(onClick = { onCopyValueClick(feeValue) }) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_copy),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                }
            }

            Spacer(Modifier.size(10.dp))
            TxDetailInfoCopyItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.tx_details_date),
                value = uiState.formattedDate,
                singleLine = false,
                onCopyClicked = onCopyValueClick,
            )

            uiState.tx.note.takeIf { it.isNotEmpty() }?.let { note ->
                Spacer(Modifier.size(10.dp))
                TxDetailInfoCopyItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    title = stringResource(R.string.tx_detail_note),
                    value = note,
                    singleLine = false,
                    onCopyClicked = onCopyValueClick,
                )
            }

            Spacer(Modifier.size(10.dp))
            TxDetailInfoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                title = stringResource(R.string.tx_details_txn_id),
                value = uiState.tariTxnId ?: stringResource(R.string.tx_details_txn_id_processing),
                singleLine = false,
            ) {
                if (uiState.blockExplorerLink != null) {
                    IconButton(onClick = onBlockExplorerClick) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_open_url),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                } else {
                    DotsAnimation(Modifier.size(40.dp))
                }
            }

            Spacer(Modifier.size(10.dp))
            TxDetailInfoStatusItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                txStatus = uiState.txStatusText,
            )

            Spacer(Modifier.size(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.tx_details_total),
                    style = TariDesignSystem.typography.headingLarge,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${WalletConfig.amountFormatter.format(uiState.totalAmount.tariValue)} ${uiState.ticker}",
                    style = TariDesignSystem.typography.headingLarge,
                )
            }
            Spacer(Modifier.size(24.dp))

            Spacer(Modifier.weight(1f))

            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.common_close),
                onClick = onBackClick,
            )
            if (uiState.showCancelButton) {
                Spacer(Modifier.size(16.dp))
                TariTextButton(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 20.dp),
                    text = stringResource(R.string.tx_detail_cancel_tx),
                    onClick = onCancelTxClick,
                    warningColor = true,
                )
            }
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
@Preview
private fun TxDetailsScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        TxDetailsScreen(
            uiState = TxDetailsModel.UiState(
                tx = MockDataStub.createCompletedTx(25_000_000L),
                ticker = "XTM",
                blockExplorerBaseUrl = "",
                requiredConfirmationCount = 3L,
                contact = MockDataStub.createContact(),
            ),
            onBackClick = {},
            onCancelTxClick = {},
            onCopyValueClick = {},
            onBlockExplorerClick = {},
            onContactEditClick = {},
            onFeeInfoClick = {},
            onEmojiIdDetailsClick = {},
        )
    }
}

@Composable
@Preview
private fun TxDetailsScreenPendingPreview() {
    TariDesignSystem(TariTheme.Light) {
        TxDetailsScreen(
            uiState = TxDetailsModel.UiState(
                tx = MockDataStub.createCompletedTx(
                    amount = 25_000_000L,
                    status = TxStatus.PENDING,
                ),
                ticker = "XTM",
                blockExplorerBaseUrl = "",
                requiredConfirmationCount = 3L,
                contact = MockDataStub.createContact(),
            ),
            onBackClick = {},
            onCancelTxClick = {},
            onCopyValueClick = {},
            onBlockExplorerClick = {},
            onContactEditClick = {},
            onFeeInfoClick = {},
            onEmojiIdDetailsClick = {},
        )
    }
}

@Composable
@Preview
private fun TxDetailsScreenCancelledPreview() {
    TariDesignSystem(TariTheme.Light) {
        TxDetailsScreen(
            uiState = TxDetailsModel.UiState(
                tx = MockDataStub.createCancelledTx(
                    amount = 25_000_000L,
                ),
                ticker = "XTM",
                blockExplorerBaseUrl = "",
                requiredConfirmationCount = 3L,
                contact = MockDataStub.createContact(),
            ),
            onBackClick = {},
            onCancelTxClick = {},
            onCopyValueClick = {},
            onBlockExplorerClick = {},
            onContactEditClick = {},
            onFeeInfoClick = {},
            onEmojiIdDetailsClick = {},
        )
    }
}
