package com.tari.android.wallet.ui.screen.exchange.widget

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub


@Composable
fun ExchangeStatusCard(
    transaction: Exolix.Transaction,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(transaction.statusIcon),
                contentDescription = null,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(transaction.statusTitle),
                style = TariDesignSystem.typography.headingLarge,
                color = TariDesignSystem.colors.textPrimary,
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = transaction.statusSubtitle(),
                style = TariDesignSystem.typography.body1,
                color = TariDesignSystem.colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@get:DrawableRes
private val Exolix.Transaction.statusIcon: Int
    get() = when (this.status) {
        Exolix.TransactionStatus.SUCCESS -> R.drawable.vector_exolix_status_success
        Exolix.TransactionStatus.EXCHANGING -> R.drawable.vector_exolix_status_exchanging
        Exolix.TransactionStatus.WAIT -> R.drawable.tari_construction// TODO: replace with proper icon
        Exolix.TransactionStatus.CONFIRMATION -> R.drawable.tari_construction// TODO: replace with proper icon
        Exolix.TransactionStatus.CONFIRMED -> R.drawable.tari_construction// TODO: replace with proper icon
        Exolix.TransactionStatus.SENDING -> R.drawable.tari_construction// TODO: replace with proper icon
        Exolix.TransactionStatus.OVERDUE -> R.drawable.tari_construction// TODO: replace with proper icon
        Exolix.TransactionStatus.REFUNDED -> R.drawable.tari_construction // TODO: replace with proper icon
    }

@get:StringRes
private val Exolix.Transaction.statusTitle: Int
    get() = when (this.status) {
        Exolix.TransactionStatus.WAIT -> R.string.exchange_status_card_waiting_for_deposit
        Exolix.TransactionStatus.CONFIRMATION,
        Exolix.TransactionStatus.CONFIRMED,
        Exolix.TransactionStatus.EXCHANGING,
        Exolix.TransactionStatus.SENDING -> R.string.exchange_status_card_funds_received

        Exolix.TransactionStatus.SUCCESS -> R.string.exchange_status_card_swapped
        Exolix.TransactionStatus.OVERDUE -> R.string.exchange_status_card_deposit_overdue
        Exolix.TransactionStatus.REFUNDED -> R.string.exchange_status_card_funds_refunded
    }

@Composable
private fun Exolix.Transaction.statusSubtitle(): String =
    when (this.status) {
        Exolix.TransactionStatus.WAIT -> stringResource(R.string.exchange_status_card_subtitle_wait, coinFrom.coinName)
        Exolix.TransactionStatus.CONFIRMATION,
        Exolix.TransactionStatus.CONFIRMED -> stringResource(R.string.exchange_status_card_subtitle_received, coinFrom.coinName)

        Exolix.TransactionStatus.EXCHANGING -> stringResource(R.string.exchange_status_card_subtitle_exchanging, coinFrom.coinName)
        Exolix.TransactionStatus.SENDING -> stringResource(R.string.exchange_status_card_subtitle_sending, coinFrom.coinName, coinTo.coinCode)
        Exolix.TransactionStatus.OVERDUE -> stringResource(
            R.string.exchange_status_card_subtitle_overdue,
            WalletConfig.amountFormatter.format(amount),
            coinFrom.coinCode,
        )

        Exolix.TransactionStatus.REFUNDED -> stringResource(
            R.string.exchange_status_card_subtitle_refunded,
            WalletConfig.amountFormatter.format(amount),
            coinFrom.coinCode,
        )

        Exolix.TransactionStatus.SUCCESS -> stringResource(
            R.string.exchange_status_card_subtitle_success,
            WalletConfig.amountFormatter.format(amount),
            coinFrom.coinCode,
            WalletConfig.amountFormatter.format(amountTo),
            coinTo.coinCode,
        )
    }

@Composable
@Preview
private fun ExchangeStatusCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp),
        ) {
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.WAIT,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.CONFIRMATION,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.CONFIRMED,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.EXCHANGING,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.SENDING,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.SUCCESS,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.OVERDUE,
                ),
            )
            Spacer(Modifier.size(16.dp))
            ExchangeStatusCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                transaction = MockDataStub.createExchangeTransaction(
                    status = Exolix.TransactionStatus.REFUNDED,
                ),
            )
            Spacer(Modifier.size(16.dp))
        }
    }
}
