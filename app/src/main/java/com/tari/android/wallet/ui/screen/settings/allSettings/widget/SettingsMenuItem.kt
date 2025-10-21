package com.tari.android.wallet.ui.screen.settings.allSettings.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.screen.settings.allSettings.Setting
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun SettingsMenuItem(
    setting: Setting,
    textColor: Color = TariDesignSystem.colors.textPrimary,
    endIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    SettingsMenuItem(
        title = stringResource(setting.titleRes),
        textColor = textColor,
        endIcon = endIcon,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
fun SettingsMenuItem(
    title: String,
    textColor: Color = TariDesignSystem.colors.textPrimary,
    endIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 20.dp, horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            style = TariDesignSystem.typography.body1.copy(color = textColor),
        )
        Spacer(Modifier.size(20.dp))
        endIcon?.let {
            endIcon()
            Spacer(Modifier.size(8.dp))
        }
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(R.drawable.vector_arrow_right),
            contentDescription = null,
            tint = TariDesignSystem.colors.componentsNavbarIcons,
        )
    }
}

@Composable
@Preview
private fun SettingsMenuItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SettingsMenuItem(
            setting = Setting.Biometrics,
            onClick = {},
        )

        SettingsMenuItem(
            setting = Setting.DeleteWallet,
            textColor = TariDesignSystem.colors.errorMain,
            onClick = {},
        )
        SettingsMenuItem(
            setting = Setting.DeleteWallet,
            onClick = {},
            endIcon = {
                Image(
                    painter = painterResource(R.drawable.vector_positive_check),
                    contentDescription = null,
                )
            },
        )
        SettingsMenuItem(
            setting = Setting.DeleteWallet,
            onClick = {},
            endIcon = { TariProgressView(Modifier.size(24.dp)) },
        )
    }
}