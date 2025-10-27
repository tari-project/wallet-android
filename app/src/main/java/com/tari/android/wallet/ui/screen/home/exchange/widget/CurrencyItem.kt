package com.tari.android.wallet.ui.screen.home.exchange.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun CurrencyItem(
    title: String,
    subtitle: String,
    iconUrl: String?,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    showArrow: Boolean = false,
    defaut: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        colors = CardDefaults.cardColors(if (onClick != null) TariDesignSystem.colors.backgroundPrimary else TariDesignSystem.colors.backgroundSecondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = if (selected) BorderStroke(1.dp, TariDesignSystem.colors.secondaryMain) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = onClick != null, onClick = onClick ?: {})
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                modifier = Modifier.size(32.dp),
                model = iconUrl,
                contentDescription = title,
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = TariDesignSystem.typography.body1,
                    )
                    if (defaut) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.exchange_network_default_label),
                            style = TariDesignSystem.typography.body2,
                            color = TariDesignSystem.colors.secondaryMain,
                        )
                    }
                }
                Text(
                    text = subtitle,
                    style = TariDesignSystem.typography.body2,
                    color = TariDesignSystem.colors.textSecondary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (showArrow) {
                Spacer(modifier = Modifier.size(16.dp))
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.vector_arrow_right),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarIcons,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewCurrencyItemVariants() {
    PreviewSecondarySurface(TariTheme.Light) {
        Spacer(modifier = Modifier.size(16.dp))
        CurrencyItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Bitcoin",
            subtitle = "BTC",
            iconUrl = "https://cryptologos.cc/logos/bitcoin-btc-logo.png",
            selected = false,
            showArrow = false,
            onClick = {},
        )
        Spacer(modifier = Modifier.size(12.dp))
        CurrencyItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Bitcoin",
            subtitle = "BTC",
            iconUrl = "https://cryptologos.cc/logos/bitcoin-btc-logo.png",
            selected = false,
            showArrow = false,
            defaut = true,
            onClick = {},
        )
        Spacer(modifier = Modifier.size(12.dp))
        CurrencyItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Bitcoin",
            subtitle = "BTC",
            iconUrl = "https://cryptologos.cc/logos/bitcoin-btc-logo.png",
            selected = true,
            showArrow = false,
            onClick = {},
        )
        Spacer(modifier = Modifier.size(12.dp))
        CurrencyItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Ethereum",
            subtitle = "ETH",
            iconUrl = "https://cryptologos.cc/logos/ethereum-eth-logo.png",
            selected = false,
            showArrow = false,
        )
        Spacer(modifier = Modifier.size(12.dp))
        CurrencyItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = "Tari",
            subtitle = "TARI",
            iconUrl = "https://cryptologos.cc/logos/tari-tari-logo.png",
            selected = false,
            showArrow = true,
            onClick = {},
        )
        Spacer(modifier = Modifier.size(16.dp))
    }
}
