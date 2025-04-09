package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariModalBottomSheet
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun BalanceInfoModal(
    onDismiss: () -> Unit,
    totalBalance: MicroTari,
    availableBalance: MicroTari,
    ticker: String,
    modifier: Modifier = Modifier,
) {
    TariModalBottomSheet(
        modifier = modifier,
        onDismiss = onDismiss,
    ) { animatedDismiss ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
        ) {
            Row(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = WalletConfig.balanceFormatter.format(totalBalance.tariValue),
                    style = TextStyle(
                        fontSize = 40.sp,
                        lineHeight = 40.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight(600),
                        color = TariDesignSystem.colors.textPrimary,
                    ),
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = ticker,
                    style = TariDesignSystem.typography.heading2XLarge,
                )
            }
            IconButton(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopEnd)
                    .size(48.dp),
                onClick = animatedDismiss,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.vector_common_close),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarIcons,
                )
            }
        }

        Spacer(modifier = Modifier.size(24.dp))
        TariHorizontalDivider(Modifier.padding(horizontal = 40.dp))
        Spacer(modifier = Modifier.size(24.dp))
        Row(modifier = Modifier.padding(horizontal = 40.dp)) {
            Text(
                text = stringResource(R.string.home_balance_info_modal_total_balance),
                style = TariDesignSystem.typography.headingLarge,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${WalletConfig.amountFormatter.format(totalBalance.tariValue)} $ticker",
                style = TariDesignSystem.typography.headingLarge,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            modifier = Modifier.padding(horizontal = 40.dp),
            text = stringResource(R.string.home_balance_info_modal_total_balance_description),
            style = TariDesignSystem.typography.body1,
        )

        Spacer(modifier = Modifier.size(24.dp))
        TariHorizontalDivider(Modifier.padding(horizontal = 40.dp))
        Spacer(modifier = Modifier.size(24.dp))
        Row(modifier = Modifier.padding(horizontal = 40.dp)) {
            Text(
                text = stringResource(R.string.home_balance_info_modal_available_balance),
                style = TariDesignSystem.typography.headingLarge,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "${WalletConfig.amountFormatter.format(availableBalance.tariValue)} $ticker",
                style = TariDesignSystem.typography.headingLarge,
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            modifier = Modifier.padding(horizontal = 40.dp),
            text = stringResource(R.string.home_balance_info_modal_available_balance_description),
            style = TariDesignSystem.typography.body1,
        )
        Spacer(modifier = Modifier.size(40.dp))
    }
}

@Composable
@Preview
private fun BalanceInfoModalPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        BalanceInfoModal(
            onDismiss = {},
            totalBalance = 4_836_150_000.toMicroTari(),
            availableBalance = 2_583_150_000.toMicroTari(),
            ticker = "XTM",
        )
    }
}