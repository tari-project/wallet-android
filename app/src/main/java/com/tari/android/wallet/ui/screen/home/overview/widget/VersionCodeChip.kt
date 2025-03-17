package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tari.android.wallet.data.ConnectionIndicatorState
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariVerticalDivider
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun VersionCodeChip(
    networkName: String,
    ffiVersion: String,
    connectionIndicatorState: ConnectionIndicatorState,
    onVersionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .wrapContentSize()
            .height(20.dp)
            .clip(TariDesignSystem.shapes.chip)
            .border(width = 1.dp, color = TariDesignSystem.colors.elevationOutlined, shape = TariDesignSystem.shapes.chip)
            .background(color = TariDesignSystem.colors.backgroundPrimary)
            .clickable(onClick = onVersionClick)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    when (connectionIndicatorState) {
                        ConnectionIndicatorState.Connected -> TariDesignSystem.colors.systemGreen
                        ConnectionIndicatorState.ConnectedWithIssues -> TariDesignSystem.colors.systemYellow
                        ConnectionIndicatorState.Disconnected -> TariDesignSystem.colors.systemRed
                    }
                ),
        )
        TariVerticalDivider(modifier = Modifier.padding(vertical = 5.dp))
        Text(
            text = networkName,
            style = TextStyle(
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 27.sp,
                color = TariDesignSystem.colors.textPrimary,
            ),
        )
        Text(
            text = ffiVersion,
            style = TextStyle(
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 27.sp,
                color = TariDesignSystem.colors.textSecondary,
            ),
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
@Preview
private fun VersionCodeChipPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        VersionCodeChip(
            modifier = Modifier.padding(16.dp),
            networkName = "NextNet",
            ffiVersion = "v1.11.0-rc.0",
            connectionIndicatorState = ConnectionIndicatorState.ConnectedWithIssues,
            onVersionClick = {},
        )
    }
}