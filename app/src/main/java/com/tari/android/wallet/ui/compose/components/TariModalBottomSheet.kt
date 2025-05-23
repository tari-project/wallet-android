package com.tari.android.wallet.ui.compose.components

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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import kotlinx.coroutines.launch

private val modalSheetScrimColor = Color(0x80000000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariModalBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(animatedDismiss: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val animatedDismiss: () -> Unit = {
        scope
            .launch { bottomSheetState.hide() }
            .invokeOnCompletion {
                if (!bottomSheetState.isVisible) {
                    onDismiss()
                }
            }
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color.Transparent,
        scrimColor = modalSheetScrimColor,
        dragHandle = null,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 10.dp,
            shape = TariDesignSystem.shapes.bottomSheet,
            color = TariDesignSystem.colors.backgroundPopup,
        ) {
            Column {
                content(animatedDismiss)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TariModalImageBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    topImageHeight: Dp = 0.dp, // this height is needed to calculate content padding workaround to show the top picture with a "negative" top padding
    topImage: @Composable BoxScope.() -> Unit = {},
    content: @Composable ColumnScope.(animatedDismiss: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val animatedDismiss: () -> Unit = {
        scope
            .launch { bottomSheetState.hide() }
            .invokeOnCompletion {
                if (!bottomSheetState.isVisible) {
                    onDismiss()
                }
            }
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = Color.Transparent,
        scrimColor = modalSheetScrimColor,
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

                    content(animatedDismiss)
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

@Composable
@Preview
private fun PreviewTariBottomModalDialog() {
    PreviewSecondarySurface(TariTheme.Light) {
        TariModalBottomSheet(
            onDismiss = {},
        ) { animatedDismiss ->
            Text(
                modifier = Modifier.padding(20.dp),
                text = "Content"
            )
        }
    }
}
