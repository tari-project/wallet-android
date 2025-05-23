package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun BlockSyncChip(
    walletScannedHeight: Int,
    chainTip: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .wrapContentSize(align = Alignment.CenterStart)
            .height(26.dp)
            .clip(TariDesignSystem.shapes.button)
            .background(color = TariDesignSystem.colors.backgroundAccent)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val isSyncing = walletScannedHeight == 0 || walletScannedHeight != chainTip

        if (isSyncing) {
            TariProgressView(Modifier.size(14.dp))
        } else {
            Image(
                painter = painterResource(id = R.drawable.vector_block_synced),
                contentDescription = null,
            )
        }

        if (isSyncing) {
            Text(
                text = stringResource(R.string.home_block_sync_syncing),
                style = TextStyle(
                    fontSize = 11.sp,
                    lineHeight = 27.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight(600),
                    color = TariDesignSystem.colors.textPrimary,
                ),
            )
            if (chainTip != 0) {
                Text(
                    text = stringResource(R.string.home_block_sync_blocks_remaining, chainTip - walletScannedHeight),
                    style = TextStyle(
                        fontSize = 11.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight(500),
                        color = TariDesignSystem.colors.textPrimary,
                    ),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.home_block_sync_synced, chainTip),
                style = TextStyle(
                    fontSize = 11.sp,
                    lineHeight = 27.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight(600),
                    color = TariDesignSystem.colors.textPrimary,
                ),
            )
        }
    }
}

@Composable
@Preview
private fun BlockSyncChipPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        BlockSyncChip(
            walletScannedHeight = 0,
            chainTip = 0,
            modifier = Modifier.padding(16.dp),
        )

        BlockSyncChip(
            walletScannedHeight = 666,
            chainTip = 1009,
            modifier = Modifier.padding(16.dp),
        )

        BlockSyncChip(
            walletScannedHeight = 1009,
            chainTip = 1009,
            modifier = Modifier.padding(16.dp),
        )
    }
}