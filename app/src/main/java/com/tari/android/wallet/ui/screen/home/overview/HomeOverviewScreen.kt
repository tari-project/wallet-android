package com.tari.android.wallet.ui.screen.home.overview

import androidx.compose.foundation.Image
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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
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
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariInheritTextButton
import com.tari.android.wallet.ui.compose.widgets.StartMiningButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun HomeOverviewScreen(
    uiState: HomeOverviewModel.UiState,
    onStartMiningClicked: () -> Unit,
    onSendTariClicked: () -> Unit,
    onRequestTariClicked: () -> Unit,
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
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                text = stringResource(R.string.home_title_tari_universe),
                style = TariDesignSystem.typography.heading2XLarge,
            )
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
        }
    }
}

@Composable
private fun ActiveMinersCard(
    modifier: Modifier,
    isMining: Boolean,
    activeMinersCount: Int?,
    onStartMiningClicked: () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        color = Color.Black,
    ) {
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
            Spacer(modifier = Modifier.width(8.dp))
            StartMiningButton(
                isMining = isMining,
                onClick = onStartMiningClicked,
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
    Surface(
        modifier = modifier.height(200.dp),
        shape = TariDesignSystem.shapes.card,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .paint(
                    painter = painterResource(R.drawable.tari_balance_card_background),
                    contentScale = ContentScale.FillBounds
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
                        modifier = Modifier.clickable(onClick = { /* TODO */ }),
                        painter = painterResource(id = R.drawable.vector_home_overview_hide_balance),
                        contentDescription = null,
                    )
                }
                Row {
                    Text(
                        modifier = Modifier.alignByBaseline(),
                        text = WalletConfig.balanceFormatter.format(balance.availableBalance.tariValue),
                        style = TextStyle(
                            fontFamily = PoppinsFontFamily,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 1.em,
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
                            lineHeight = 4.em,
                            color = Color.White,
                        ),
                    )
                }
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
                avatarEmoji = "🐱",
                emojiMedium = "🐶",
                isMining = false,
                balance = BalanceInfo(
                    availableBalance = 4_836_150_000.toMicroTari(),
                    pendingIncomingBalance = 0.toMicroTari(),
                    pendingOutgoingBalance = 0.toMicroTari(),
                    timeLockedBalance = 0.toMicroTari(),
                ),
                ticker = "tXTR",
            ),
            onStartMiningClicked = {},
            onSendTariClicked = {},
            onRequestTariClicked = {},
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