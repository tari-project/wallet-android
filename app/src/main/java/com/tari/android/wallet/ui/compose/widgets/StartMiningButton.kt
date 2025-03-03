package com.tari.android.wallet.ui.compose.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewPrimarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.WithRippleColor
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun StartMiningButton(
    isMining: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!isMining) {
        WithRippleColor(Color.Black) {
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = TariDesignSystem.colors.systemGreen,
                    disabledBackgroundColor = TariDesignSystem.colors.actionDisabledBackground,
                ),
                shape = TariDesignSystem.shapes.startMiningButton,
                modifier = modifier.defaultMinSize(minHeight = 42.dp, minWidth = 124.dp),
            ) {
                Text(
                    text = stringResource(R.string.home_active_miners_start_mining),
                    color = Color.Black,
                    style = TariDesignSystem.typography.buttonSmall,
                )
            }
        }
    } else {
        WithRippleColor(TariDesignSystem.colors.systemGreen) {
            // TODO values are hardcoded now. Probably need to be used the values from the palette
            val buttonBackgroundBrush = Brush.radialGradient(
                colors = listOf(
                    Color(0x14FFFFFF), // rgba(255, 255, 255, 0.08)
                    Color(0x00FFFFFF)  // rgba(255, 255, 255, 0.00)
                ),
                center = androidx.compose.ui.geometry.Offset(150f, 20f),
                radius = 150f,
            )
            val buttonBorderBrush = Brush.horizontalGradient(listOf(Color(0x3302FE63), Color(0x0802FE63)))

            OutlinedButton(
                onClick = onClick,
                border = BorderStroke(
                    width = 1.dp,
                    brush = buttonBorderBrush,
                ),
                shape = TariDesignSystem.shapes.startMiningButton,
                colors = ButtonDefaults.outlinedButtonColors(
                    backgroundColor = Color.Transparent,
                    disabledContentColor = Color.Transparent,
                ),
                contentPadding = PaddingValues(),
                modifier = modifier,
            ) {
                Box(
                    modifier = Modifier
                        .background(buttonBackgroundBrush)
                        .defaultMinSize(minHeight = 42.dp, minWidth = 124.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.home_active_miners_you_mining),
                        color = TariDesignSystem.colors.systemGreen,
                        style = TariDesignSystem.typography.buttonSmall,
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun StartMiningButtonPreview() {
    PreviewPrimarySurface(TariTheme.Dark) {
        Column {
            StartMiningButton(
                isMining = false,
                modifier = Modifier.padding(20.dp),
            )

            StartMiningButton(
                isMining = true,
                modifier = Modifier.padding(20.dp),
            )
        }
    }
}
