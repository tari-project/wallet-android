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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.giphy.sdk.analytics.GiphyPingbacks.context
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.shortString
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.Locale

private const val TX_ITEM_DATE_FORMAT = "E, MMM d"

@Composable
fun TxItem(
    modifier: Modifier = Modifier,
    txDto: TxDto,
    ticker: String,
    onTxClick: () -> Unit,
) {
    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onTxClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color = TariDesignSystem.colors.componentsNavbarIcons), contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    painter = painterResource(R.drawable.tari_sample_avatar),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarBackground,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = txDto.itemMessage(),
                    style = TariDesignSystem.typography.headingMedium,
                )
                Text(
                    text = txDto.tx.dateTime.txListItemFormattedDate(),
                    style = TariDesignSystem.typography.body2,
                )
            }
            Row {
                Text(
                    text = when {
                        txDto.tx.isInbound -> "+"
                        txDto.tx.isOutbound -> "-"
                        else -> ""
                    },
                    style = TariDesignSystem.typography.headingLarge,
                    color = when {
                        txDto.tx.isInbound -> TariDesignSystem.colors.systemGreen
                        txDto.tx.isOutbound -> TariDesignSystem.colors.systemRed
                        else -> TariDesignSystem.colors.textPrimary
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = WalletConfig.amountFormatter.format(txDto.tx.amount.tariValue) + " " + ticker,
                    style = TariDesignSystem.typography.headingLarge,
                )
            }
        }
    }
}

@Composable
fun DateTime.txListItemFormattedDate(): String {
    val txDate = this.toLocalDate()
    val todayDate = LocalDate.now()
    val yesterdayDate = todayDate.minusDays(1)
    return when {
        txDate.isEqual(todayDate) -> {
            val minutesSinceTx = Minutes.minutesBetween(this, DateTime.now()).minutes
            when {
                minutesSinceTx == 0 -> stringResource(R.string.tx_list_now)
                minutesSinceTx < 60 -> stringResource(R.string.tx_list_minutes_ago, minutesSinceTx)
                else -> stringResource(R.string.tx_list_hours_ago, Hours.hoursBetween(this, DateTime.now()).hours)
            }
        }

        txDate.isEqual(yesterdayDate) -> stringResource(R.string.home_tx_list_header_yesterday)
        else -> txDate.toString(TX_ITEM_DATE_FORMAT, Locale.ENGLISH)
    }
}

@Composable
fun TxDto.itemMessage(): String {
    val txUser = tx.tariContact
    return when {
        tx.isCoinbase -> {
            when (tx.direction) {
                Tx.Direction.INBOUND -> stringResource(R.string.tx_details_coinbase_inbound)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_details_coinbase_outbound)
            }
        }

        contact != null && contact.contactInfo.getAlias().isNotEmpty() || txUser.walletAddress.isUnknownUser() -> {
            val alias = contact?.contactInfo?.getAlias().orEmpty().ifBlank { context.getString(R.string.unknown_source) }
            when (tx.direction) {
                Tx.Direction.INBOUND -> stringResource(R.string.tx_list_sent_a_payment, alias)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid_with_alias, alias)
            }
        }

        tx.isOneSided -> {
            (stringResource(R.string.tx_list_someone) + " " + stringResource(R.string.tx_list_paid_you))
        }

        else -> { // display emoji id
            when (tx.direction) {
                Tx.Direction.INBOUND -> txUser.walletAddress.shortString() + " " + stringResource(R.string.tx_list_paid_you)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid) + " " + txUser.walletAddress.shortString()
            }
        }
    }
}

@Preview
@Composable
private fun TxItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TxItem(
            modifier = Modifier.padding(16.dp),
            txDto = MockDataStub.createTxDto(
                amount = 122334455,
                contactAlias = "Alice",
            ),
            ticker = "XTM",
            onTxClick = {},
        )
    }
}