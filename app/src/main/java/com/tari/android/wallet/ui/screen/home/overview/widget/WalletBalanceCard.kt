package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun WalletBalanceCard(
    balance: BalanceInfo,
    ticker: String,
    onBalanceHelpClicked: () -> Unit,
    onHideBalanceClicked: () -> Unit,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(200.dp),
        shape = TariDesignSystem.shapes.card,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 16.dp),
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
                        modifier = Modifier.clickable(onClick = onHideBalanceClicked),
                        painter = painterResource(id = R.drawable.vector_home_overview_hide_balance),
                        contentDescription = null,
                    )
                }
                if (isBalanceHidden) {
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
                        BasicText(
                            modifier = Modifier
                                .alignByBaseline()
                                .weight(1f, false),
                            text = WalletConfig.balanceFormatter.format(balance.totalBalance.tariValue),
                            maxLines = 1,
                            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 56.sp, stepSize = 1.sp),
                            style = TextStyle(
                                fontFamily = PoppinsFontFamily,
                                fontSize = 56.sp,
                                lineHeight = 56.sp,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            R.string.home_available_to_spend_balance,
                            if (isBalanceHidden) {
                                stringResource(R.string.home_wallet_balance_hidden)
                            } else {
                                WalletConfig.balanceFormatter.format(balance.availableBalance.tariValue) + " " + ticker
                            },
                        ),
                        style = TextStyle(
                            fontFamily = PoppinsFontFamily,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 26.sp,
                            color = Color.White.copy(alpha = 0.5f),
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(color = Color(0x33FFFFFF))
                            .clickable(onClick = onBalanceHelpClicked),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "?",
                            style = TextStyle(
                                fontSize = 15.sp,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Normal,
                                color = Color(0xFFE5E5E5),
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun BalanceCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        WalletBalanceCard(
            modifier = Modifier.padding(16.dp),
            balance = BalanceInfo(
                availableBalance = 2_240_836_150_222_222_222.toMicroTari(),
                pendingIncomingBalance = 0.toMicroTari(),
                pendingOutgoingBalance = 0.toMicroTari(),
                timeLockedBalance = 4_836_150_000.toMicroTari(),
            ),
            ticker = "XTM",
            onBalanceHelpClicked = {},
            onHideBalanceClicked = {},
            isBalanceHidden = false,
        )

        WalletBalanceCard(
            modifier = Modifier.padding(16.dp),
            balance = BalanceInfo(
                availableBalance = 24_836.toMicroTari(),
                pendingIncomingBalance = 0.toMicroTari(),
                pendingOutgoingBalance = 0.toMicroTari(),
                timeLockedBalance = 4_836_150_000.toMicroTari(),
            ),
            ticker = "XTM",
            onBalanceHelpClicked = {},
            onHideBalanceClicked = {},
            isBalanceHidden = false,
        )

        WalletBalanceCard(
            modifier = Modifier.padding(16.dp),
            balance = BalanceInfo(
                availableBalance = 0.toMicroTari(),
                pendingIncomingBalance = 0.toMicroTari(),
                pendingOutgoingBalance = 0.toMicroTari(),
                timeLockedBalance = 0.toMicroTari(),
            ),
            ticker = "XTM",
            onBalanceHelpClicked = {},
            onHideBalanceClicked = {},
            isBalanceHidden = false,
        )

        WalletBalanceCard(
            modifier = Modifier.padding(16.dp),
            balance = BalanceInfo(
                availableBalance = 0.toMicroTari(),
                pendingIncomingBalance = 0.toMicroTari(),
                pendingOutgoingBalance = 0.toMicroTari(),
                timeLockedBalance = 0.toMicroTari(),
            ),
            ticker = "XTM",
            onBalanceHelpClicked = {},
            onHideBalanceClicked = {},
            isBalanceHidden = true,
        )
    }
}