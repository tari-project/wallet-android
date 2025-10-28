package com.tari.android.wallet.ui.screen.home.exchange.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun SelectedCurrencyChip(
    title: String,
    subtitle: String,
    iconUrl: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .wrapContentSize(align = Alignment.CenterStart)
            .clip(TariDesignSystem.shapes.button)
            .background(color = TariDesignSystem.colors.actionFocus)
            .clickable(onClick = onClick ?: {}, enabled = onClick != null)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            modifier = Modifier.size(28.dp),
            model = iconUrl,
            contentDescription = title,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column() {
            Text(
                text = title,
                style = TariDesignSystem.typography.buttonSmall,
                color = TariDesignSystem.colors.textPrimary,
            )
            Text(
                text = subtitle,
                style = TariDesignSystem.typography.body2,
                color = TariDesignSystem.colors.textSecondary,
            )
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TariDesignSystem.colors.textPrimary,
            )
        } else {
            Spacer(modifier = Modifier.width(20.dp))
        }
    }
}

@Preview
@Composable
private fun PreviewSelectedCurrencyCard() {
    PreviewSecondarySurface(TariTheme.Light) {
        SelectedCurrencyChip(
            modifier = Modifier.padding(16.dp),
            title = "ETH",
            subtitle = "Arbitrum",
            iconUrl = "https://cryptologos.cc/logos/ethereum-eth-logo.png",
            onClick = {},
        )
        SelectedCurrencyChip(
            modifier = Modifier.padding(16.dp),
            title = "XTM",
            subtitle = "Tari",
            iconUrl = "https://exolix.com/icons/networks/Tari_1755361659372.png",
        )
    }
}

@Preview
@Composable
private fun PreviewSelectedCurrencyDarkCard() {
    PreviewSecondarySurface(TariTheme.Dark) {
        SelectedCurrencyChip(
            modifier = Modifier.padding(16.dp),
            title = "ETH",
            subtitle = "Arbitrum",
            iconUrl = "https://cryptologos.cc/logos/ethereum-eth-logo.png",
            onClick = {},
        )
        SelectedCurrencyChip(
            modifier = Modifier.padding(16.dp),
            title = "XTM",
            subtitle = "Tari",
            iconUrl = "https://exolix.com/icons/networks/Tari_1755361659372.png",
        )
    }
}