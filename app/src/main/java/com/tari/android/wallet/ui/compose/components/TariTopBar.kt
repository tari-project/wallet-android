package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun TariTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    showShadow: Boolean = true,
    action: (@Composable BoxScope.() -> Unit)? = null,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        color = TariDesignSystem.colors.backgroundSecondary,
        shadowElevation = if (showShadow) 2.dp else 0.dp,
    ) {
        Box {
            if (onBack != null) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .size(48.dp), onClick = onBack
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_back_button),
                        contentDescription = null,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                    )
                }
            }

            Text(
                text = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = if (onBack != null) 64.dp else 16.dp)
                    .padding(8.dp),
                style = TariDesignSystem.typography.headingLarge,
                textAlign = TextAlign.Center,
            )

            if (action != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    content = action,
                )
            }
        }
    }
}

@Composable
@Preview
fun TariTopBarShadowPreview() {
    PreviewSecondarySurface(TariTheme.Light, Modifier.fillMaxSize()) {
        Column {
            TariTopBar(
                title = "Toolbar Title",
                onBack = {},
                showShadow = true,
                action = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_share),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                },
            )
        }
    }
}

@Composable
@Preview
fun TariTopBarPreview() {
    PreviewSecondarySurface(TariTheme.Light, Modifier.fillMaxSize()) {
        Column {
            TariTopBar(
                title = "Toolbar Title",
                onBack = {},
                showShadow = false,
            )
        }
    }
}

@Composable
@Preview
fun TariTopBarLongTitlePreview() {
    PreviewSecondarySurface(TariTheme.Light, Modifier.fillMaxSize()) {
        Column {
            TariTopBar(
                title = "Toolbar very long title that should be truncated at some point and not overflow the screen",
                onBack = {},
                showShadow = false,
                action = {
                    IconButton(onClick = {}) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_share),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                },
            )
        }
    }
}