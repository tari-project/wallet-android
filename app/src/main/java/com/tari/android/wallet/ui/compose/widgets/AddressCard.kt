package com.tari.android.wallet.ui.compose.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.compose.PreviewPrimarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.base58Ellipsized
import com.tari.android.wallet.util.emojiIdEllipsized


@Composable
fun AddressCard(
    address: TariWalletAddress,
    cardTitle: String,
    onEmojiCopyClick: () -> Unit,
    onBase58CopyClick: () -> Unit,
    onEmojiDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showFullBase58 by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clip(TariDesignSystem.shapes.chip)
            .background(color = TariDesignSystem.colors.backgroundAccent)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.size(16.dp))
        Text(
            text = cardTitle,
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
                text = address.emojiIdEllipsized(),
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
                modifier = Modifier
                    .weight(1f)
                    .clickable { showFullBase58 = !showFullBase58 },
                text = if (showFullBase58) address.fullBase58 else address.base58Ellipsized(),
                style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
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
private fun AddressCardPreview() {
    PreviewPrimarySurface(TariTheme.Light) {
        AddressCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            address = MockDataStub.WALLET_ADDRESS,
            cardTitle = stringResource(R.string.receive_tari_your_address),
            onEmojiCopyClick = {},
            onBase58CopyClick = {},
            onEmojiDetailClick = {},
        )
    }
}