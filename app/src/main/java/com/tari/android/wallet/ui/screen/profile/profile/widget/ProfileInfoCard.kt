package com.tari.android.wallet.ui.screen.profile.profile.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.toMicroTari
import java.math.BigDecimal
import java.text.DecimalFormat

@Composable
fun TariMinedCard(
    balance: BigDecimal,
    ticker: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = TariDesignSystem.shapes.card,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 10.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.vector_profile_tari_mined_logo),
                contentDescription = null,
            )
            Spacer(Modifier.weight(1f))
            Row {
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = WalletConfig.balanceFormatter.format(balance),
                    style = TariDesignSystem.typography.modalTitleLarge,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    modifier = Modifier.alignByBaseline(),
                    text = ticker,
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
            }
            Text(
                text = stringResource(R.string.airdrop_profile_tari_mined),
                style = TariDesignSystem.typography.body2,
            )
        }
    }
}

@Composable
fun GemsEarnedCard(
    gemsCount: Double,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = TariDesignSystem.shapes.card,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 10.dp),
        ) {
            Image(
                modifier = Modifier.size(27.dp),
                painter = painterResource(id = R.drawable.vector_profile_gems_earned_logo),
                contentDescription = null,
            )
            Spacer(Modifier.weight(1f))

            Text(
                text = DecimalFormat("#,###,###").format(gemsCount),
                style = TariDesignSystem.typography.modalTitleLarge,
            )
            Text(
                text = stringResource(R.string.airdrop_profile_gems_earned),
                style = TariDesignSystem.typography.body2,
            )
        }
    }
}

@Composable
@Preview
fun TariMinedCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TariMinedCard(
            modifier = Modifier
                .padding(16.dp)
                .height(120.dp)
                .width(240.dp),
            balance = 24_836_150_000.toMicroTari().tariValue,
            ticker = "tXTR",
        )
    }
}

@Composable
@Preview
fun GemsEarnedCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        GemsEarnedCard(
            modifier = Modifier
                .padding(16.dp)
                .height(120.dp)
                .width(240.dp),
            gemsCount = 24_836_150.0,
        )
    }
}