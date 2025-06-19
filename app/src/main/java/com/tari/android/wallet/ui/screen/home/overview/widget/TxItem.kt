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
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.safeCastTo
import com.tari.android.wallet.util.shortString
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.Locale

private const val TX_ITEM_DATE_FORMAT = "E, MMM d"
private val MIN_ROUNDING = 10000.toBigInteger()

@Composable
fun TxItem(
    modifier: Modifier = Modifier,
    txDto: TxDto,
    ticker: String,
    onTxClick: () -> Unit,
    balanceHidden: Boolean = false,
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
                    text = if (balanceHidden) {
                        stringResource(R.string.home_wallet_balance_hidden)
                    } else {
                        (txDto.tx.amount.takeIf { it.value >= MIN_ROUNDING }?.tariValue?.let { WalletConfig.balanceFormatter.format(it) }
                            ?: "<0.01") + " " + ticker
                    },
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
    return when {
        tx.isCoinbase -> {
            stringResource(R.string.tx_details_coinbase, tx.safeCastTo<CompletedTx>()?.minedHeight ?: "--")
        }

        !contact.alias.isNullOrEmpty() -> {
            when (tx.direction) {
                Tx.Direction.INBOUND -> stringResource(R.string.tx_list_sent_a_payment, contact.alias)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid_with_alias, contact.alias)
            }
        }

        contact.walletAddress.isUnknownUser() -> {
            when (tx.direction) {
                Tx.Direction.INBOUND -> stringResource(R.string.tx_list_sent_a_payment, stringResource(R.string.unknown_source))
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid_with_alias, stringResource(R.string.unknown_source))
            }
        }

        // TODO as far as we use only OSP tx, all the txs are OSP, but we have contact address of them
//        tx.isOneSided -> {
//            when (tx.direction) {
//                Tx.Direction.INBOUND -> stringResource(R.string.tx_list_someone) + " " + stringResource(R.string.tx_list_paid_you)
//                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid) + " " + stringResource(R.string.tx_list_someone).lowercase()
//            }
//        }

        else -> { // display emoji id
            when (tx.direction) {
                Tx.Direction.INBOUND -> contact.walletAddress.shortString() + " " + stringResource(R.string.tx_list_paid_you)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid) + " " + contact.walletAddress.shortString()
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
                amount = 12345678,
                contactAlias = "Alice",
            ),
            ticker = "XTM",
            balanceHidden = false,
            onTxClick = {},
        )

        TxItem(
            modifier = Modifier.padding(16.dp),
            txDto = MockDataStub.createTxDto(
                amount = 12345,
                contactAlias = "Alice",
            ),
            ticker = "XTM",
            balanceHidden = false,
            onTxClick = {},
        )

        TxItem(
            modifier = Modifier.padding(16.dp),
            txDto = MockDataStub.createTxDto(
                amount = 1234,
                contactAlias = "Alice",
            ),
            ticker = "XTM",
            balanceHidden = false,
            onTxClick = {},
        )

        TxItem(
            modifier = Modifier.padding(16.dp),
            txDto = MockDataStub.createTxDto(
                amount = 1234,
                contactAlias = "Alice",
            ),
            ticker = "XTM",
            balanceHidden = true,
            onTxClick = {},
        )
    }
}