package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariModalImageBottomSheet
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariVerticalGradient
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun SyncSuccessModal(
    onDismiss: () -> Unit,
) {
    SyncSuccessModalBase(
        onDismiss = onDismiss,
    ) { animatedDismiss ->
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_sync_dialog_title),
            style = TariDesignSystem.typography.modalTitleLarge,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.home_sync_dialog_description),
            style = TariDesignSystem.typography.body1,
        )
        Spacer(Modifier.height(16.dp))
        TariPrimaryButton(
            text = stringResource(R.string.home_sync_dialog_button),
            onClick = animatedDismiss,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun RestoreSuccessModal(
    onDismiss: () -> Unit,
) {
    SyncSuccessModalBase(
        onDismiss = onDismiss,
    ) { animatedDismiss ->
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recovery_success_dialog_title),
            style = TariDesignSystem.typography.modalTitleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            modifier = Modifier.padding(horizontal = 20.dp),
            text = stringResource(R.string.recovery_success_dialog_description),
            style = TariDesignSystem.typography.body1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        TariPrimaryButton(
            text = stringResource(R.string.recovery_success_dialog_close),
            onClick = animatedDismiss,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SyncSuccessModalBase(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(animatedDismiss: () -> Unit) -> Unit,
) {
    TariModalImageBottomSheet(
        onDismiss = onDismiss,
        topImageHeight = 300.dp,
        topImage = {
            Image(
                modifier = Modifier.fillMaxWidth(),
                painter = painterResource(R.drawable.tari_tower_coins_helix_cropped),
                contentDescription = null,
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.FillWidth,
            )
            TariVerticalGradient(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .height(100.dp)
                    .fillMaxWidth(),
                from = Color.Transparent,
                to = TariDesignSystem.colors.backgroundPopup,
            )
        },
    ) { animatedDismiss ->
        content(animatedDismiss)
    }
}


@Preview
@Composable
private fun SyncSuccessModalPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SyncSuccessModal {}
    }
}

@Preview
@Composable
private fun RestoreSuccessModalPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        RestoreSuccessModal {}
    }
}