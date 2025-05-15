package com.tari.android.wallet.ui.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier,
    isMining: Boolean = false,
    onStartMiningClick: () -> Unit = {},
) {
    if (!isMining) {
        NotMiningView(modifier)
    } else {
        MiningView(modifier)
    }
}

@Composable
private fun NotMiningButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WithRippleColor(Color.Black) {
        Button(
            modifier = modifier.defaultMinSize(minHeight = 42.dp, minWidth = 124.dp),
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = TariDesignSystem.colors.systemGreen,
                disabledContainerColor = TariDesignSystem.colors.actionDisabledBackground,
            ),
            shape = TariDesignSystem.shapes.startMiningButton,
        ) {
            Text(
                text = stringResource(R.string.home_active_miners_start_mining),
                color = Color.Black,
                style = TariDesignSystem.typography.buttonSmall,
            )
        }
    }
}

@Composable
private fun MiningView(
    modifier: Modifier = Modifier,
) {
    val buttonBackgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0x14FFFFFF), // rgba(255, 255, 255, 0.08)
            Color(0x00FFFFFF)  // rgba(255, 255, 255, 0.00)
        ),
        center = androidx.compose.ui.geometry.Offset(150f, 20f),
        radius = 150f,
    )
    val buttonBorderBrush = Brush.horizontalGradient(listOf(Color(0x3302FE63), Color(0x0802FE63)))

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = buttonBorderBrush, shape = TariDesignSystem.shapes.startMiningButton,
            )
            .background(buttonBackgroundBrush)
            .defaultMinSize(minHeight = 42.dp, minWidth = 124.dp),
        contentAlignment = Alignment.Center,
    ) {

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.home_active_miners_you_mining),
            color = TariDesignSystem.colors.systemGreen,
            style = TariDesignSystem.typography.buttonSmall,
        )
    }
}

@Composable
private fun NotMiningView(
    modifier: Modifier = Modifier,
) {

    val buttonBackgroundBrush = Brush.radialGradient(
        colors = listOf(
            Color(0x14FFFFFF), // rgba(255, 255, 255, 0.08)
            Color(0x00FFFFFF)  // rgba(255, 255, 255, 0.00)
        ),
        center = androidx.compose.ui.geometry.Offset(150f, 20f),
        radius = 150f,
    )
    val buttonBorderBrush = Brush.horizontalGradient(listOf(Color(0x33FF3232), Color(0x08FF3232)))

    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                brush = buttonBorderBrush, shape = TariDesignSystem.shapes.startMiningButton,
            )
            .background(buttonBackgroundBrush)
            .defaultMinSize(minHeight = 42.dp, minWidth = 145.dp),
        contentAlignment = Alignment.Center,
    ) {

        Text(
            modifier = Modifier.padding(horizontal = 16.dp),
            text = stringResource(R.string.home_active_miners_you_not_mining),
            color = TariDesignSystem.colors.systemRed,
            style = TariDesignSystem.typography.buttonSmall,
        )
    }
}

@Composable
@Preview
private fun StartMiningButtonPreview() {
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
