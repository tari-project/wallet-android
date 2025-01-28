package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.OutlinedButton
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.PreviewSurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun TariPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: TariButtonSize = TariButtonSize.Large,
    modifier: Modifier = Modifier,
) {
    WithRippleColor(TariDesignSystem.colors.buttonPrimaryText) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = TariDesignSystem.colors.buttonPrimaryBackground,
                disabledBackgroundColor = TariDesignSystem.colors.actionDisabledBackground,
            ),
            shape = TariDesignSystem.shapes.button,
            modifier = modifier.defaultMinSize(minHeight = size.minHeight()),
            enabled = enabled,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = size.textHorizontalPadding()),
                color = if (enabled) TariDesignSystem.colors.buttonPrimaryText else TariDesignSystem.colors.actionDisabled,
                style = size.textStyle(),
            )
        }
    }
}

@Composable
fun TariSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: TariButtonSize = TariButtonSize.Large,
    modifier: Modifier = Modifier,
) {
    WithRippleColor(Color.Black) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = TariDesignSystem.colors.primaryMain,
                disabledBackgroundColor = TariDesignSystem.colors.actionDisabledBackground,
            ),
            shape = TariDesignSystem.shapes.button,
            modifier = modifier.defaultMinSize(minHeight = size.minHeight()),
            enabled = enabled,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = size.textHorizontalPadding()),
                color = if (enabled) Color.Black else TariDesignSystem.colors.actionDisabled,
                style = size.textStyle(),
            )
        }
    }
}

@Composable
fun TariOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    size: TariButtonSize = TariButtonSize.Large,
    modifier: Modifier = Modifier,
) {
    WithRippleColor(TariDesignSystem.colors.textPrimary) {
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) TariDesignSystem.colors.buttonPrimaryBackground else TariDesignSystem.colors.actionDisabledBackground,
            ),
            shape = TariDesignSystem.shapes.button,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, disabledContentColor = Color.Transparent),
            modifier = modifier.defaultMinSize(minHeight = size.minHeight()),
            enabled = enabled,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = size.textHorizontalPadding()),
                color = if (enabled) TariDesignSystem.colors.textPrimary else TariDesignSystem.colors.actionDisabled,
                style = size.textStyle(),
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TariInheritTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    WithRippleColor(TariDesignSystem.colors.textPrimary) {
        OutlinedButton(
            onClick = onClick,
            border = BorderStroke(
                width = 1.dp,
                color = if (enabled) TariDesignSystem.colors.divider else TariDesignSystem.colors.actionDisabledBackground,
            ),
            shape = TariDesignSystem.shapes.button,
            colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent, disabledContentColor = Color.Transparent),
            modifier = modifier.defaultMinSize(minHeight = TariButtonSize.Medium.minHeight()),
            enabled = enabled,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = TariButtonSize.Medium.textHorizontalPadding()),
                color = if (enabled) TariDesignSystem.colors.textPrimary else TariDesignSystem.colors.actionDisabled,
                style = TariButtonSize.Medium.textStyle(),
            )
        }
    }
}

@Composable
fun TariTextButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    WithRippleColor(TariDesignSystem.colors.textPrimary) {
        TextButton(
            onClick = onClick,
            shape = TariDesignSystem.shapes.button,
            modifier = modifier.defaultMinSize(minHeight = TariButtonSize.Medium.minHeight()),
            enabled = enabled,
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = TariButtonSize.Medium.textHorizontalPadding()),
                color = if (enabled) TariDesignSystem.colors.textPrimary else TariDesignSystem.colors.actionDisabled,
                style = TariButtonSize.Medium.textStyle(),
            )
        }
    }
}

enum class TariButtonSize {
    Small, Medium, Large;
}

@Composable
private fun TariButtonSize.textStyle() = when (this) {
    TariButtonSize.Small -> TariDesignSystem.textStyles.buttonSmall
    TariButtonSize.Medium -> TariDesignSystem.textStyles.buttonMedium
    TariButtonSize.Large -> TariDesignSystem.textStyles.buttonLarge
}

@Composable
private fun TariButtonSize.minHeight() = when (this) {
    TariButtonSize.Small -> 30.dp
    TariButtonSize.Medium -> 36.dp
    TariButtonSize.Large -> 50.dp
}

@Composable
private fun TariButtonSize.textHorizontalPadding() = when (this) {
    TariButtonSize.Small -> 10.dp
    TariButtonSize.Medium -> 16.dp
    TariButtonSize.Large -> 22.dp
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WithRippleColor(
    rippleColor: Color,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(
            color = rippleColor,
            rippleAlpha = RippleAlpha(
                draggedAlpha = 0.3f,
                focusedAlpha = 0.3f,
                hoveredAlpha = 0.3f,
                pressedAlpha = 0.3f,
            )
        ),
        content = content,
    )
}


@Preview
@Composable
private fun TariButtonsPreview() {
    PreviewSurface(TariTheme.Dark) {
        Column {
            TariPrimaryButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Primary Button Large",
                onClick = { },
            )

            TariPrimaryButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Primary Button Medium",
                size = TariButtonSize.Medium,
                onClick = { },
            )

            TariPrimaryButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                text = "Primary Button Small",
                size = TariButtonSize.Small,
                onClick = { },
            )

            TariPrimaryButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Primary Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariSecondaryButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Secondary Button",
                onClick = { },
            )

            TariSecondaryButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Secondary Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariOutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Outlined Button",
                onClick = { },
            )

            TariOutlinedButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                text = "Outlined Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Text Button",
                onClick = { },
            )

            TariTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Text Button Disabled",
                onClick = { },
                enabled = false,
            )

            TariInheritTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Inherit Text Button",
                onClick = { },
            )

            TariInheritTextButton(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .align(alignment = Alignment.CenterHorizontally),
                text = "Inherit Text Button Disabled",
                onClick = { },
                enabled = false,
            )
        }
    }
}
