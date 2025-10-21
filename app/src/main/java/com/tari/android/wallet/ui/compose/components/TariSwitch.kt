package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun TariSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = thumbContent,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = TariDesignSystem.colors.primaryContrast,
            checkedTrackColor = TariDesignSystem.colors.primaryMain,
            uncheckedThumbColor = TariDesignSystem.colors.actionDisabled,
            uncheckedTrackColor = TariDesignSystem.colors.backgroundSecondary,
        ),
    )
}

@Composable
@Preview
private fun TariSwitchPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        var checked by remember { mutableStateOf(true) }
        TariSwitch(
            modifier = Modifier.padding(20.dp),
            checked = checked,
            onCheckedChange = { checked = it },
        )
        TariSwitch(
            modifier = Modifier.padding(20.dp),
            checked = !checked,
            onCheckedChange = { checked = !it },
        )
    }
}

@Composable
@Preview
private fun TariSwitchDarkPreview() {
    PreviewSecondarySurface(TariTheme.Dark) {
        var checked by remember { mutableStateOf(true) }
        TariSwitch(
            modifier = Modifier.padding(20.dp),
            checked = checked,
            onCheckedChange = { checked = it },
        )
        TariSwitch(
            modifier = Modifier.padding(20.dp),
            checked = !checked,
            onCheckedChange = { checked = !it },
        )
    }
}