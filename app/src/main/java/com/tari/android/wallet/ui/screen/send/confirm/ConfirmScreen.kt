package com.tari.android.wallet.ui.screen.send.confirm

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import com.tari.android.wallet.model.TransactionData
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.send.confirm.widget.SenderCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoAddressItem
import com.tari.android.wallet.ui.screen.tx.details.widget.TxDetailInfoItem
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun ConfirmScreen(
    uiState: ConfirmViewModel.UiState,
    onBackClick: () -> Unit,
    onCopyValueClick: (value: String) -> Unit,
    onConfirmClick: () -> Unit,
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
                title = uiState.screenTitle,
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            Spacer(Modifier.size(36.dp))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.confirm_tx_you_are_about_to_send),
                style = TariDesignSystem.typography.headingLarge,
            )
            Spacer(Modifier.size(8.dp))
            Box {
                Column {
                    SenderCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        title = "${WalletConfig.amountFormatter.format(uiState.transactionData.amount.tariValue)} ${uiState.ticker}",
                        iconRes = R.drawable.vector_gem,
                    )
                    Spacer(Modifier.size(8.dp))
                    SenderCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        title = uiState.screenTitle,
                        iconRes = R.drawable.vector_gem,
                    )
                }
                Image(
                    modifier = Modifier.align(Alignment.Center),
                    painter = painterResource(R.drawable.vector_tx_detail_arrow_down),
                    contentDescription = null,
                )
            }

            Spacer(Modifier.size(24.dp))
            TxDetailInfoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                title = stringResource(R.string.tx_detail_transaction_fee),
                value = "${WalletConfig.amountFormatter.format(uiState.transactionData.feePerGram.tariValue)} ${uiState.ticker}",
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
            TxDetailInfoAddressItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                title = stringResource(R.string.tx_details_recipient_address),
                walletAddress = uiState.transactionData.recipientContact.walletAddress,
                onCopyClicked = onCopyValueClick,
                onEmojiIdDetailsClick = onEmojiIdDetailsClick,
            )

            Spacer(Modifier.size(10.dp))
            TxDetailInfoItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp),
                title = stringResource(R.string.tx_detail_note),
                value = uiState.transactionData.note.orEmpty(),
                singleLine = false,
            )

            Spacer(Modifier.size(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
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

            Spacer(Modifier.weight(1f))

            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.confirm_tx_confirm_and_send_button),
                onClick = onConfirmClick,
            )
            Spacer(Modifier.size(16.dp))
            TariTextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.common_cancel),
                onClick = onBackClick,
            )
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
@Preview
private fun ConfirmScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ConfirmScreen(
            uiState = ConfirmViewModel.UiState(
                ticker = "XTM",
                transactionData = TransactionData(
                    amount = 1200000.toMicroTari(),
                    feePerGram = 1000.toMicroTari(),
                    note = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.",
                    recipientContact = MockDataStub.createContact(),
                    isOneSidePayment = true,
                ),
            ),
            onBackClick = {},
            onConfirmClick = {},
            onCopyValueClick = {},
            onFeeInfoClick = {},
            onEmojiIdDetailsClick = {},
        )
    }
}