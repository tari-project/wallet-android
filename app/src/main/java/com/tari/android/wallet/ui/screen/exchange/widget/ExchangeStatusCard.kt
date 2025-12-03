package com.tari.android.wallet.ui.screen.exchange.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
                text = transaction.statusTitle(),
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

        Exolix.TransactionStatus.WAIT,
        Exolix.TransactionStatus.CONFIRMATION,
        Exolix.TransactionStatus.CONFIRMED,
        Exolix.TransactionStatus.EXCHANGING,
        Exolix.TransactionStatus.SENDING -> R.drawable.vector_exolix_status_exchanging

        Exolix.TransactionStatus.OVERDUE,
        Exolix.TransactionStatus.REFUNDED -> R.drawable.vector_exolix_status_error
    }

@Composable
private fun Exolix.Transaction.statusTitle(): String =
    when (this.status) {
        Exolix.TransactionStatus.WAIT -> stringResource(R.string.exchange_status_card_waiting_for_deposit)
        Exolix.TransactionStatus.CONFIRMATION,
        Exolix.TransactionStatus.CONFIRMED -> stringResource(R.string.exchange_status_card_funds_received)

        Exolix.TransactionStatus.EXCHANGING -> stringResource(R.string.exchange_status_card_deposits_in_progress)
        Exolix.TransactionStatus.SENDING -> stringResource(R.string.exchange_status_card_sending, coinTo.coinCode)
        Exolix.TransactionStatus.SUCCESS -> stringResource(R.string.exchange_status_card_exchange_complete)
        Exolix.TransactionStatus.OVERDUE -> stringResource(R.string.exchange_status_card_transaction_expired)
        Exolix.TransactionStatus.REFUNDED -> stringResource(R.string.exchange_status_card_transaction_refunded)
    }

@Composable
private fun Exolix.Transaction.statusSubtitle(): String =
    when (this.status) {
        Exolix.TransactionStatus.WAIT -> stringResource(R.string.exchange_status_card_subtitle_wait, coinFrom.coinCode)
        Exolix.TransactionStatus.CONFIRMATION,
        Exolix.TransactionStatus.CONFIRMED -> stringResource(R.string.exchange_status_card_subtitle_received, coinFrom.coinCode)

        Exolix.TransactionStatus.EXCHANGING -> stringResource(R.string.exchange_status_card_subtitle_exchanging, coinFrom.coinCode)
        Exolix.TransactionStatus.SENDING -> stringResource(R.string.exchange_status_card_subtitle_sending, coinTo.coinCode)
        Exolix.TransactionStatus.OVERDUE -> stringResource(R.string.exchange_status_card_subtitle_overdue)
        Exolix.TransactionStatus.REFUNDED -> stringResource(R.string.exchange_status_card_subtitle_refunded, coinFrom.coinCode)
        Exolix.TransactionStatus.SUCCESS -> stringResource(R.string.exchange_status_card_subtitle_success, coinTo.coinCode)
    }

@Composable
@Preview
private fun ExchangeStatusCardPreview_Wait() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.WAIT,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Confirmation() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.CONFIRMATION,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Confirmed() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.CONFIRMED,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Exchanging() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.EXCHANGING,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Sending() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.SENDING,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Success() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.SUCCESS,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Overdue() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.OVERDUE,
            ),
        )
    }
}

@Composable
@Preview
private fun ExchangeStatusCardPreview_Refunded() {
    PreviewSecondarySurface(TariTheme.Light) {
        ExchangeStatusCard(
            modifier = Modifier.padding(16.dp),
            transaction = MockDataStub.createExchangeTransaction(
                status = Exolix.TransactionStatus.REFUNDED,
            ),
        )
    }
}