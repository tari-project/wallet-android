package com.tari.android.wallet.ui.compose

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun PreviewPrimarySurface(
    theme: TariTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TariDesignSystem(theme) {
        Surface(
            modifier = modifier,
            color = TariDesignSystem.colors.backgroundPrimary,
            content = content,
        )
    }
}

@Composable
fun PreviewSecondarySurface(
    theme: TariTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    TariDesignSystem(theme) {
        Surface(
            modifier = modifier,
            color = TariDesignSystem.colors.backgroundSecondary,
            content = content,
        )
    }
}