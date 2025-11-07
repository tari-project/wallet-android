package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.exolix.Exolix
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@Composable
fun PendingExolixTxItem(
    transaction: Exolix.TransactionResponse,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color = TariDesignSystem.colors.componentsNavbarIcons),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(90f),
                    painter = painterResource(R.drawable.vector_two_arrows_circle),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarBackground,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.home_pending_exolix_transaction_title_format,
                        transaction.amount.stripTrailingZeros().toPlainString(),
                        transaction.coinFrom.coinCode,
                        transaction.amountTo.stripTrailingZeros().toPlainString(),
                        transaction.coinTo.coinCode,
                    ),
                    style = TariDesignSystem.typography.headingMedium,
                )
                Text(
                    text = transaction.status.statusText(),
                    style = TariDesignSystem.typography.body2,
                )
            }
            Icon(
                modifier = Modifier.size(20.dp),
                painter = painterResource(R.drawable.vector_arrow_right),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
            )
        }
    }
}

@Composable
private fun Exolix.TransactionStatus.statusText(): String = stringResource(
    when (this) {
        Exolix.TransactionStatus.WAIT -> R.string.exchange_status_wait
        Exolix.TransactionStatus.CONFIRMATION -> R.string.exchange_status_confirmation
        Exolix.TransactionStatus.CONFIRMED -> R.string.exchange_status_confirmed
        Exolix.TransactionStatus.EXCHANGING -> R.string.exchange_status_exchanging
        Exolix.TransactionStatus.SENDING -> R.string.exchange_status_sending
        Exolix.TransactionStatus.SUCCESS -> R.string.exchange_status_success
        Exolix.TransactionStatus.OVERDUE -> R.string.exchange_status_overdue
        Exolix.TransactionStatus.REFUNDED -> R.string.exchange_status_refunded
    }
)

@Composable
@Preview
private fun PendingExolixTxItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        PendingExolixTxItem(
            transaction = MockDataStub.createTransactionResponse(
                status = Exolix.TransactionStatus.WAIT,
            ),
            modifier = Modifier.padding(16.dp),
            onClick = {},
        )
    }
}
