package com.tari.android.wallet.ui.compose

import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun TariDesignSystem(
    theme: TariTheme,
    content: @Composable () -> Unit,
) {
    TariDesignSystem(
        theme = when (theme) {
            TariTheme.AppBased -> {
                when (LocalContext.current.resources.configuration.uiMode and UI_MODE_NIGHT_MASK) {
                    UI_MODE_NIGHT_YES -> LocalTariTheme.Dark
                    else -> LocalTariTheme.Light
                }
            }

            TariTheme.Light -> LocalTariTheme.Light
            TariTheme.Dark -> LocalTariTheme.Dark
        },
        content = content,
    )
}

@Composable
private fun TariDesignSystem(
    theme: LocalTariTheme,
    content: @Composable () -> Unit,
) {
    val tariColors = when (theme) {
        LocalTariTheme.Light -> TariLightColorPalette
        LocalTariTheme.Dark -> TariDarkColorPalette
    }

    val tariTextStyle = TariTextStyles(
        headingLarge = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            color = tariColors.textPrimary,
        ),
        headingMedium = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp,
            color = tariColors.textPrimary,
        ),
        headingSmall = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 18.sp,
            color = tariColors.textPrimary,
        ),
        body1 = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 21.sp,
            color = tariColors.textSecondary,
        ),
        body2 = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 17.sp,
            color = tariColors.textSecondary,
        ),
        modalTitle = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            color = tariColors.textPrimary,
        ),
        menuItem = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 27.sp,
            color = tariColors.textPrimary,
        ),
        buttonText = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 21.sp,
            color = tariColors.textPrimary,
        ),
        buttonLarge = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 26.sp,
            color = tariColors.textPrimary,
            letterSpacing = 0.4.sp,
        ),
        buttonMedium = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 24.sp,
            color = tariColors.textPrimary,
            letterSpacing = 0.4.sp,
        ),
        buttonSmall = TextStyle(
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp,
            color = tariColors.textPrimary,
            letterSpacing = 0.4.sp,
        ),
        linkSpan = TextLinkStyles(
            style = SpanStyle(
                textDecoration = TextDecoration.Underline,
            ),
        ),
    )

    CompositionLocalProvider(
        LocalTariColors provides tariColors,
        LocalTariShapes provides TariShapes(),
        LocalTariTextStyles provides tariTextStyle,
    ) {
        MaterialTheme(
            content = content,
        )
    }
}

object TariDesignSystem {
    val typography: TariTextStyles
        @Composable
        get() = LocalTariTextStyles.current
    val shapes: TariShapes
        @Composable
        get() = LocalTariShapes.current
    val colors: TariColors
        @Composable
        get() = LocalTariColors.current
}

private enum class LocalTariTheme {
    Light,
    Dark,
}