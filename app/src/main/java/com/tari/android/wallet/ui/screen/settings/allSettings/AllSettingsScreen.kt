package com.tari.android.wallet.ui.screen.settings.allSettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun AllSettingsScreen(
    uiState: AllSettingsViewModel.UiState,
    onSettingClick: (option: Setting) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.size(30.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.all_settings_page_title),
                style = TariDesignSystem.typography.heading2XLarge,
            )
            Spacer(Modifier.size(40.dp))

            SettingsItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                setting = Setting.Profile,
                onClick = { onSettingClick(Setting.Profile) },
            )
            TariHorizontalDivider(Modifier.padding(horizontal = 20.dp))
            SettingsItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                setting = Setting.Contacts,
                onClick = { onSettingClick(Setting.Contacts) },
            )
            TariHorizontalDivider(Modifier.padding(horizontal = 20.dp))
            SettingsItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                setting = Setting.WalletSettings,
                onClick = { onSettingClick(Setting.WalletSettings) },
            )
            TariHorizontalDivider(Modifier.padding(horizontal = 20.dp))
            SettingsItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                setting = Setting.Support,
                onClick = { onSettingClick(Setting.Support) },
            )
            TariHorizontalDivider(Modifier.padding(horizontal = 20.dp))
            SettingsItem(
                modifier = Modifier.padding(horizontal = 20.dp),
                setting = Setting.Legal,
                onClick = { onSettingClick(Setting.Legal) },
            )

            Spacer(Modifier.weight(1f))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                text = uiState.versionText,
                style = TariDesignSystem.typography.body2,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.size(20.dp))
        }
    }
}

@Composable
fun SettingsItem(
    setting: Setting,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            painter = painterResource(setting.iconRes),
            contentDescription = null,
            tint = TariDesignSystem.colors.componentsNavbarIcons.copy(alpha = 0.5f),
        )
        Spacer(Modifier.size(20.dp))
        Text(
            text = stringResource(setting.titleRes),
            style = TariDesignSystem.typography.menuItem,
        )
    }
}

@Composable
@Preview
private fun AllSettingsScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        AllSettingsScreen(
            uiState = AllSettingsViewModel.UiState(
                versionText = "TESTNET v0.27.0 (b776)",
            ),
            onSettingClick = {},
        )
    }
}