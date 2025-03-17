package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.widgets.StartMiningButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.DebugConfig

@Composable
fun ActiveMinersCard(
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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