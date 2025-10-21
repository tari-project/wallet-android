package com.tari.android.Support.ui.screen.settings.allSettings.support

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.allSettings.Setting
import com.tari.android.wallet.ui.screen.settings.allSettings.support.SupportSettingsViewModel
import com.tari.android.wallet.ui.screen.settings.allSettings.widget.SettingsMenuItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun SupportSettingsScreen(
    uiState: SupportSettingsViewModel.UiState,
    onBackClick: () -> Unit,
    onSettingClick: (option: Setting) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.all_settings_menu_support),
                onBack = onBackClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .clip(TariDesignSystem.shapes.card)
                .border(width = 1.dp, color = TariDesignSystem.colors.elevationOutlined, shape = TariDesignSystem.shapes.card)
                .background(color = TariDesignSystem.colors.backgroundPrimary),
        ) {
            SettingsMenuItem(
                setting = Setting.ReportBug,
                onClick = { onSettingClick(Setting.ReportBug) },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.VisitTari,
                onClick = { onSettingClick(Setting.VisitTari) },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.Contribute,
                onClick = { onSettingClick(Setting.Contribute) },
            )
            if (uiState.showBlockExplorer) {
                TariHorizontalDivider()
                SettingsMenuItem(
                    setting = Setting.BlockExplorer,
                    onClick = { onSettingClick(Setting.BlockExplorer) },
                )
            }
            if (uiState.showTtlStore) {
                TariHorizontalDivider()
                SettingsMenuItem(
                    setting = Setting.TtlStore,
                    onClick = { onSettingClick(Setting.TtlStore) },
                )
            }
        }
    }
}

@Composable
@Preview
private fun AllSettingsScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SupportSettingsScreen(
            uiState = SupportSettingsViewModel.UiState(
                showTtlStore = true,
                showBlockExplorer = true,
            ),
            onBackClick = {},
            onSettingClick = {},
        )
    }
}