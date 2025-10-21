package com.tari.android.wallet.ui.screen.settings.allSettings.walletSettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.allSettings.Setting
import com.tari.android.wallet.ui.screen.settings.allSettings.widget.SettingsMenuItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun WalletSettingsScreen(
    uiState: WalletSettingsViewModel.UiState,
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
                title = stringResource(R.string.all_settings_menu_wallet_settings),
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
                setting = Setting.Backup,
                onClick = { onSettingClick(Setting.Backup) },
                endIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        uiState.backupState.Text()
                        Spacer(Modifier.size(8.dp))
                        uiState.backupState.Icon()
                    }
                },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.DataCollection,
                onClick = { onSettingClick(Setting.DataCollection) },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.ScreenRecording,
                onClick = { onSettingClick(Setting.ScreenRecording) },
                endIcon = {
                    if (uiState.screenRecordingWarning) {
                        Image(
                            painter = painterResource(R.drawable.vector_warning_exclamation),
                            contentDescription = null,
                        )
                    }
                }
            )
            TariHorizontalDivider()
            if (uiState.pinCodeIsSet) {
                SettingsMenuItem(
                    setting = Setting.ChangePasscode,
                    onClick = { onSettingClick(Setting.ChangePasscode) },
                )
            } else {
                SettingsMenuItem(
                    setting = Setting.CreatePasscode,
                    onClick = { onSettingClick(Setting.CreatePasscode) },
                )
            }
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.Biometrics,
                onClick = { onSettingClick(Setting.Biometrics) },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.SelectTheme,
                onClick = { onSettingClick(Setting.SelectTheme) },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.SelectNetwork,
                onClick = { onSettingClick(Setting.SelectNetwork) },
            )
            TariHorizontalDivider()
            SettingsMenuItem(
                setting = Setting.DeleteWallet,
                textColor = TariDesignSystem.colors.errorMain,
                onClick = { onSettingClick(Setting.DeleteWallet) },
            )
        }
    }
}

@Composable
private fun BackupState.Text() {
    Text(
        text = stringResource(
            when (this) {
                is BackupState.BackupDisabled -> R.string.back_up_wallet_backup_status_disabled
                is BackupState.BackupFailed -> R.string.back_up_wallet_backup_status_outdated
                is BackupState.BackupInProgress -> R.string.back_up_wallet_backup_status_in_progress
                is BackupState.BackupUpToDate -> R.string.back_up_wallet_backup_status_up_to_date
            }
        ),
        color = when (this) {
            is BackupState.BackupDisabled,
            is BackupState.BackupFailed -> TariDesignSystem.colors.errorMain

            is BackupState.BackupInProgress -> TariDesignSystem.colors.textSecondary
            is BackupState.BackupUpToDate -> TariDesignSystem.colors.systemGreen
        },
        style = TariDesignSystem.typography.body1,
    )
}

@Composable
private fun BackupState.Icon() {
    when (this) {
        is BackupState.BackupDisabled,
        is BackupState.BackupFailed -> {
            Image(
                painter = painterResource(R.drawable.vector_warning_exclamation),
                contentDescription = null,
            )
        }

        is BackupState.BackupInProgress -> {
            TariProgressView(Modifier.size(24.dp))
        }

        is BackupState.BackupUpToDate -> {
            Image(
                painter = painterResource(R.drawable.vector_positive_check),
                contentDescription = null,
            )
        }
    }
}

@Composable
@Preview
private fun AllSettingsScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        WalletSettingsScreen(
            uiState = WalletSettingsViewModel.UiState(
                backupState = BackupState.BackupDisabled,
                pinCodeIsSet = true,
                screenRecordingWarning = true,
            ),
            onBackClick = {},
            onSettingClick = {},
        )
    }
}