package com.tari.android.wallet.ui.screen.send.receive

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveScreen(
    uiState: ReceiveViewModel.UiState,
    onBackClick: () -> Unit,
    onEmojiCopyClick: () -> Unit,
    onBase58CopyClick: () -> Unit,
    onEmojiDetailClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.receive_tari_topbar_title),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Spacer(Modifier.size(60.dp))
            NetworkTitle(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                ticker = uiState.ticker,
                networkName = uiState.networkName,
            )
            Spacer(Modifier.size(32.dp))
            QrCodeCard(
                qrBitmap = uiState.qrBitmap,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.size(32.dp))
            AddressCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                address = uiState.tariAddress,
                onEmojiCopyClick = onEmojiCopyClick,
                onBase58CopyClick = onBase58CopyClick,
                onEmojiDetailClick = onEmojiDetailClick,
            )
            Spacer(Modifier.weight(1f))
            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.common_share),
                onClick = onShareClick,
            )
            Spacer(Modifier.size(32.dp))
        }
    }
}

@Composable
private fun NetworkTitle(ticker: String, networkName: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.vector_gem),
            contentDescription = null,
            tint = TariDesignSystem.colors.textPrimary,
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = ticker,
            style = TariDesignSystem.typography.heading2XLarge,
        )
        Spacer(Modifier.size(8.dp))
        Box(
            modifier = Modifier
                .wrapContentSize()
                .height(20.dp)
                .clip(TariDesignSystem.shapes.chip)
                .background(color = TariDesignSystem.colors.backgroundAccent)
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = networkName,
                style = TariDesignSystem.typography.body2,
            )
        }
    }
}

@Composable
private fun QrCodeCard(qrBitmap: Bitmap?, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.size(232.dp),
    ) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (qrBitmap != null) {
                Image(
                    modifier = modifier.fillMaxSize(),
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = null,
                )
            } else {
                CircularProgressIndicator(
                    color = TariDesignSystem.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
fun AddressCard(
    address: TariWalletAddress,
    onEmojiCopyClick: () -> Unit,
    onBase58CopyClick: () -> Unit,
    onEmojiDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(TariDesignSystem.shapes.chip)
            .background(color = TariDesignSystem.colors.backgroundAccent)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(16.dp))
        Text(
            text = stringResource(R.string.receive_tari_your_address),
            style = TariDesignSystem.typography.body1,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onEmojiDetailClick),
                text = address.fullEmojiId,
                style = TariDesignSystem.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            TariPrimaryButton(
                size = TariButtonSize.Small,
                text = stringResource(R.string.common_copy),
                onClick = onEmojiCopyClick,
            )
        }
        Spacer(Modifier.size(8.dp))
        TariHorizontalDivider()
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = address.fullBase58,
                style = TariDesignSystem.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.size(8.dp))
            TariPrimaryButton(
                size = TariButtonSize.Small,
                text = stringResource(R.string.common_copy),
                onClick = onBase58CopyClick,
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
@Preview
fun ReceiveScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ReceiveScreen(
            uiState = ReceiveViewModel.UiState(
                ticker = "XTM",
                networkName = "Mainnet",
                tariAddress = MockDataStub.WALLET_ADDRESS,
                qrBitmap = BitmapFactory.decodeResource(LocalContext.current.resources, R.drawable.tari_splash_screen),
            ),
            onBackClick = {},
            onEmojiCopyClick = {},
            onBase58CopyClick = {},
            onEmojiDetailClick = {},
            onShareClick = {},
        )
    }
}