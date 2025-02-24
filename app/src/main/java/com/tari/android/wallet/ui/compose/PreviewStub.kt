package com.tari.android.wallet.ui.compose

import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun PreviewPrimarySurface(
    theme: TariTheme,
    content: @Composable () -> Unit,
) {
    TariDesignSystem(theme) {
        Surface(color = TariDesignSystem.colors.backgroundPrimary) { content() }
    }
}

@Composable
fun PreviewSecondarySurface(
    theme: TariTheme,
    content: @Composable () -> Unit,
) {
    TariDesignSystem(theme) {
        Surface(color = TariDesignSystem.colors.backgroundSecondary) { content() }
    }
}