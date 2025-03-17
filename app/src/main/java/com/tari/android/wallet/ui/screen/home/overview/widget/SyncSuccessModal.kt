@file:OptIn(ExperimentalMaterial3Api::class)

package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariVerticalGradient
import kotlinx.coroutines.launch

@Composable
fun SyncSuccessModal(
    onDismiss: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SyncSuccessModalBase(
        onDismiss = onDismiss,
        bottomSheetState = bottomSheetState,
    ) {
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
            onClick = {
                scope
                    .launch { bottomSheetState.hide() }
                    .invokeOnCompletion {
                        if (!bottomSheetState.isVisible) {
                            onDismiss()
                        }
                    }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun RestoreSuccessModal(
    onDismiss: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    SyncSuccessModalBase(
        onDismiss = onDismiss,
        bottomSheetState = bottomSheetState,
    ) {
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
            onClick = {
                scope
                    .launch { bottomSheetState.hide() }
                    .invokeOnCompletion {
                        if (!bottomSheetState.isVisible) {
                            onDismiss()
                        }
                    }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SyncSuccessModalBase(
    onDismiss: () -> Unit = {},
    bottomSheetState: SheetState,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBase(
        onDismiss = onDismiss,
        bottomSheetState = bottomSheetState,
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
        content = content,
    )
}

@Composable
private fun ModalBase(
    onDismiss: () -> Unit = {},
    bottomSheetState: SheetState,
    topImageHeight: Dp = 0.dp, // this height is needed to calculate content padding workaround to show the top picture with a "negative" top padding
    topImage: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color.Transparent,
        dragHandle = null,
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topImageHeight / 2),
                shadowElevation = 10.dp,
                shape = TariDesignSystem.shapes.bottomSheet,
                color = TariDesignSystem.colors.backgroundPopup,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(topImageHeight / 2))

                    content()
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp)
                    .height(topImageHeight),
                content = topImage,
            )
        }
    }
}

@Preview
@Composable
private fun SyncSuccessModalPreview() {
    SyncSuccessModal()
}

@Preview
@Composable
private fun RestoreSuccessModalPreview() {
    RestoreSuccessModal()
}