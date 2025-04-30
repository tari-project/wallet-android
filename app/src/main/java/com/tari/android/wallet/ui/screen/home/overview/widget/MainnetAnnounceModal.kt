package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.components.TariModalBottomSheet
import com.tari.android.wallet.ui.compose.components.TariOutlinedButton
import com.tari.android.wallet.ui.compose.components.TariSecondaryButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun MainnetAnnounceModal(
    onDismiss: () -> Unit,
    onWatchTrailerClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TariModalBottomSheet(
        modifier = modifier,
        onDismiss = onDismiss,
    ) { animatedDismiss ->
        Box(modifier = Modifier.wrapContentSize()) {
            Image(
                painter = painterResource(R.drawable.tari_mainnet_modal_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Column {
                Row {
                    Image(
                        modifier = Modifier.padding(start = 40.dp, top = 20.dp),
                        painter = painterResource(R.drawable.vector_mainnet_modal_7years),
                        contentDescription = null,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        modifier = Modifier
                            .padding(16.dp)
                            .clip(CircleShape)
                            .background(color = Color(0x33000000))
                            .size(36.dp),
                        onClick = animatedDismiss,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.vector_common_close),
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.size(24.dp))
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 32.dp),
                    painter = painterResource(R.drawable.vector_mainnet_modal_welcome),
                    contentDescription = null,
                )
                Spacer(Modifier.size(20.dp))
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 32.dp),
                    painter = painterResource(R.drawable.vector_mainnet_modal_live),
                    contentDescription = null,
                )
                Spacer(Modifier.size(24.dp))
                Image(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 32.dp),
                    painter = painterResource(R.drawable.tari_mainnet_modal_the_future),
                    contentDescription = null,
                )

                Spacer(Modifier.height(48.dp))
                TariSecondaryButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    text = stringResource(R.string.home_mainnet_modal_lets_go_button),
                    onClick = animatedDismiss,
                )
                Spacer(Modifier.height(24.dp))
                TariOutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    text = stringResource(R.string.home_mainnet_modal_lets_watch_trailer_button),
                    onClick = onWatchTrailerClick,
                )
                Spacer(Modifier.height(24.dp))
                Image(
                    modifier = Modifier
                        .padding(end = 30.dp, bottom = 30.dp)
                        .align(Alignment.End),
                    painter = painterResource(R.drawable.vector_mainnet_modal_powered),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
@Preview
private fun MainnetAnnounceModalPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        MainnetAnnounceModal(
            onDismiss = {},
            onWatchTrailerClick = {},
        )
    }
}