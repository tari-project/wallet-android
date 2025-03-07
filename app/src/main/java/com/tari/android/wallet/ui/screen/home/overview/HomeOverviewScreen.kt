package com.tari.android.wallet.ui.screen.home.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.giphy.sdk.analytics.GiphyPingbacks.context
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariInheritTextButton
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.widgets.StartMiningButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.toMicroTari
import com.tari.android.wallet.util.shortString
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.Locale

private const val TX_ITEM_DATE_FORMAT = "E, MMM d"

@Composable
fun HomeOverviewScreen(
    uiState: HomeOverviewModel.UiState,
    onInviteFriendClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onStartMiningClicked: () -> Unit,
    onSendTariClicked: () -> Unit,
    onRequestTariClicked: () -> Unit,
    onTxClick: (txDto: TxDto) -> Unit,
    onViewAllTxsClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.home_title_tari_universe),
                    style = TariDesignSystem.typography.heading2XLarge,
                )
                IconButton(onClick = onInviteFriendClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_home_overview_invite_friend),
                        contentDescription = null,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                    )
                }
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_home_overview_notifications),
                        contentDescription = null,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))
            ActiveMinersCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                activeMinersCount = uiState.activeMinersCount,
                isMining = uiState.isMining,
                onStartMiningClicked = onStartMiningClicked,
            )
            Spacer(modifier = Modifier.height(10.dp))
            WalletBalanceCard(
                balance = uiState.balance,
                ticker = uiState.ticker,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                TariInheritTextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.send_tari_subtitle),
                    onClick = onSendTariClicked,
                )
                Spacer(modifier = Modifier.width(16.dp))
                TariInheritTextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(R.string.request_tari_subtitle),
                    onClick = onRequestTariClicked,
                )
            }
            if (uiState.txList == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(40.dp)
                        .align(Alignment.CenterHorizontally),
                    color = TariDesignSystem.colors.primaryMain,
                )
            } else if (uiState.txList.isEmpty()) {
                EmptyTxList(
                    onStartMiningClicked = onStartMiningClicked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 80.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 30.dp, bottom = 10.dp),
                            text = stringResource(R.string.home_tx_list_recent_activity),
                            style = TariDesignSystem.typography.headingXLarge,
                        )
                    }
                    items(uiState.txList.size) { index ->
                        val txItem = uiState.txList[index]
                        TxItem(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                            txDto = txItem,
                            ticker = uiState.ticker,
                            onTxClick = { onTxClick(txItem) },
                        )
                    }
                    item {
                        TariTextButton(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 20.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(align = Alignment.CenterHorizontally),
                            text = stringResource(R.string.home_transaction_view_all_txs),
                            onClick = onViewAllTxsClick,
                        )
                    }
                    item {
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveMinersCard(
    isMining: Boolean,
    activeMinersCount: Int?,
    onStartMiningClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF07160B),
            Color(0xFF0E1510),
        ),
    )

    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        elevation = 2.dp,
    ) {
        Box(modifier = Modifier.background(backgroundBrush)) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_active_miners_title),
                        color = Color.White,
                        style = TariDesignSystem.typography.body2,
                    )
                    Row(
                        modifier = Modifier.height(36.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.vector_home_overview_active_miners),
                            contentDescription = null,
                            tint = Color.White,
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        if (activeMinersCount != null) {
                            Text(
                                text = activeMinersCount.toString(),
                                color = Color.White,
                                style = TariDesignSystem.typography.heading2XLarge,
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = TariDesignSystem.colors.primaryMain,
                            )
                        }
                    }
                }
                if (DebugConfig.showActiveMinersButton) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StartMiningButton(
                        isMining = isMining,
                        onClick = onStartMiningClicked,
                    )
                }
            }

            Image(
                modifier = Modifier
                    .alpha(0.44f)
                    .align(Alignment.TopCenter)
                    .blur(radius = 70.dp),
                contentDescription = null,
                painter = painterResource(R.drawable.tari_active_miners_card_ellipse),
            )
        }
    }
}

@Composable
fun WalletBalanceCard(
    balance: BalanceInfo,
    ticker: String,
    modifier: Modifier = Modifier,
) {
    var isBalanceHidden = remember { mutableStateOf(false) }

    Card(
        modifier = modifier.height(200.dp),
        shape = TariDesignSystem.shapes.card,
        elevation = 2.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(R.drawable.tari_balance_card_background),
                    contentScale = ContentScale.FillBounds,
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.home_wallet_balance),
                        style = TextStyle(
                            fontFamily = PoppinsFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 26.sp,
                            color = Color.White.copy(alpha = 0.5f),
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Image(
                        modifier = Modifier.clickable(onClick = { isBalanceHidden.value = !isBalanceHidden.value }),
                        painter = painterResource(id = R.drawable.vector_home_overview_hide_balance),
                        contentDescription = null,
                    )
                }
                if (isBalanceHidden.value) {
                    Text(
                        text = stringResource(R.string.home_wallet_balance_hidden),
                        style = TextStyle(
                            fontFamily = PoppinsFontFamily,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 1.em,
                            color = Color.White,
                        ),
                    )
                } else {
                    Row {
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = WalletConfig.balanceFormatter.format(balance.availableBalance.tariValue),
                            style = TextStyle(
                                fontFamily = PoppinsFontFamily,
                                fontSize = 56.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            modifier = Modifier.alignByBaseline(),
                            text = ticker,
                            style = TextStyle(
                                fontFamily = PoppinsFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTxList(
    onStartMiningClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_empty_state_title),
                style = TariDesignSystem.typography.headingMedium,
            )
            Text(
                text = stringResource(R.string.home_empty_state_description),
                style = TariDesignSystem.typography.body1,
            )
            if (DebugConfig.showActiveMinersButton) {
                Spacer(modifier = Modifier.height(8.dp))
                TariPrimaryButton(
                    size = TariButtonSize.Small,
                    text = stringResource(R.string.home_empty_state_button),
                    onClick = onStartMiningClicked,
                )
            }
        }
    }
}

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
        backgroundColor = TariDesignSystem.colors.backgroundPrimary,
        border = BorderStroke(1.dp, TariDesignSystem.colors.elevationOutlined),
        elevation = 1.dp,
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

        tx.isOneSided -> {
            (stringResource(R.string.tx_list_someone) + " " + stringResource(R.string.tx_list_paid_you))
        }

        contact != null && contact.contactInfo.getAlias().isNotEmpty() || txUser.walletAddress.isUnknownUser() -> {
            val alias = contact?.contactInfo?.getAlias().orEmpty().ifBlank { context.getString(R.string.unknown_source) }
            when (tx.direction) {
                Tx.Direction.INBOUND -> stringResource(R.string.tx_list_sent_a_payment, alias)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid_with_alias, alias)
            }
        }

        else -> { // display emoji id
            when (tx.direction) {
                Tx.Direction.INBOUND -> txUser.walletAddress.shortString() + " " + stringResource(R.string.tx_list_paid_you)
                Tx.Direction.OUTBOUND -> stringResource(R.string.tx_list_you_paid) + " " + txUser.walletAddress.shortString()
            }
        }
    }
}

@Composable
@Preview
private fun HomeOverviewScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        HomeOverviewScreen(
            uiState = HomeOverviewModel.UiState(
                activeMinersCount = 10,
                isMining = false,
                balance = BalanceInfo(
                    availableBalance = 4_836_150_000.toMicroTari(),
                    pendingIncomingBalance = 0.toMicroTari(),
                    pendingOutgoingBalance = 0.toMicroTari(),
                    timeLockedBalance = 0.toMicroTari(),
                ),
                ticker = "tXTR",
                txList = MockDataStub.createTxList(),
            ),
            onInviteFriendClick = {},
            onNotificationsClick = {},
            onStartMiningClicked = {},
            onSendTariClicked = {},
            onRequestTariClicked = {},
            onTxClick = {},
            onViewAllTxsClick = {},
        )
    }
}

@Composable
@Preview
private fun ActiveMinersCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ActiveMinersCard(
            modifier = Modifier.padding(16.dp),
            activeMinersCount = 10,
            isMining = false,
            onStartMiningClicked = {},
        )
    }
}

@Composable
@Preview
private fun ActiveMinersCardNoPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ActiveMinersCard(
            modifier = Modifier.padding(16.dp),
            activeMinersCount = null,
            isMining = true,
            onStartMiningClicked = {},
        )
    }
}

@Composable
@Preview
private fun BalanceCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        WalletBalanceCard(
            modifier = Modifier.padding(16.dp),
            balance = BalanceInfo(
                availableBalance = 24_836_150_000.toMicroTari(),
                pendingIncomingBalance = 0.toMicroTari(),
                pendingOutgoingBalance = 0.toMicroTari(),
                timeLockedBalance = 0.toMicroTari(),
            ),
            ticker = "tXTR",
        )
    }
}

@Composable
@Preview
private fun EmptyTxListPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        EmptyTxList(
            onStartMiningClicked = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 48.dp),
        )
    }
}