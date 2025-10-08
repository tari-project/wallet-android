package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.tari.android.wallet.ui.compose.components.TariSwitch
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.allSettings.widget.SettingsMenuItem
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOption
import com.tari.android.wallet.ui.screen.settings.backup.data.BackupOptionDto
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Locale

private val BACKUP_DATE_FORMATTER = DateTimeFormat.forPattern("MMM dd yyyy").withLocale(Locale.ENGLISH)
private val BACKUP_TIME_FORMATTER = DateTimeFormat.forPattern("hh:mm a")

@Composable
fun BackupSettingsScreen(
    uiState: BackupSettingsViewModel.UiState,
    onBackClick: () -> Unit,
    onSeedPhraseClick: () -> Unit,
    onPasswordClick: () -> Unit,
    onUploadNowClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    onBackupCheckedChange: (checked: Boolean) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.all_settings_back_up_wallet_settings_entry),
                onBack = onBackClick,
            )
        },
    ) { paddingValues ->
        Column(Modifier.padding(paddingValues)) {
            Spacer(Modifier.size(20.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.back_up_wallet_page_description),
                style = TariDesignSystem.typography.body1,
            )
            Spacer(Modifier.size(20.dp))

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(TariDesignSystem.shapes.card)
                    .border(width = 1.dp, color = TariDesignSystem.colors.elevationOutlined, shape = TariDesignSystem.shapes.card)
                    .background(color = TariDesignSystem.colors.backgroundPrimary),
            ) {
                SettingsMenuItem(
                    title = stringResource(R.string.back_up_wallet_with_recovery_phrase_cta),
                    endIcon = {
                        if (uiState.seedPhraseWarning) {
                            Image(
                                painter = painterResource(R.drawable.vector_warning_exclamation),
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = onSeedPhraseClick,
                )
                TariHorizontalDivider()
                UploadGoogleItem(
                    progress = uiState.backupProgress,
                    checked = uiState.backupSwitchChecked,
                    lastBackupDate = uiState.lastSuccessDate,
                    onCheckedChange = onBackupCheckedChange,
                )
                TariHorizontalDivider()
                if (uiState.showPasswordButton) {
                    SettingsMenuItem(
                        title = stringResource(R.string.back_up_wallet_set_backup_password_cta),
                        onClick = onPasswordClick,
                    )
                    TariHorizontalDivider()
                }
                if (uiState.backupNowAvailable) {
                    SettingsMenuItem(
                        title = stringResource(R.string.back_up_wallet_to_cloud_cta),
                        onClick = onUploadNowClick,
                    )
                    TariHorizontalDivider()
                }
                SettingsMenuItem(
                    title = stringResource(R.string.all_settings_back_up_wallet_settings_safety),
                    onClick = onLearnMoreClick,
                )
            }
        }
    }
}

@Composable
private fun UploadGoogleItem(
    modifier: Modifier = Modifier,
    progress: Boolean,
    checked: Boolean,
    lastBackupDate: DateTime?,
    onCheckedChange: (checked: Boolean) -> Unit,
) {
//    var checked by remember { mutableStateOf(checked) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.back_up_wallet_google_title),
                style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
            )

            lastBackupDate?.toLocalDateTime()?.let { date ->
                stringResource(
                    R.string.back_up_wallet_last_successful_backup,
                    BACKUP_DATE_FORMATTER.print(date),
                    BACKUP_TIME_FORMATTER.print(date),
                )
            }?.let { dateText ->
                Spacer(Modifier.size(4.dp))

                Text(
                    text = dateText,
                    style = TariDesignSystem.typography.body1,
                )
            }
        }
        Spacer(Modifier.size(20.dp))

        if (progress) {
            TariProgressView(Modifier.size(32.dp))
        } else {
            TariSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

@Composable
@Preview
private fun AllSettingsScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        BackupSettingsScreen(
            uiState = BackupSettingsViewModel.UiState(
                backupState = BackupState.BackupDisabled,
                seedPhraseWarning = true,
                backupOption = BackupOptionDto(
                    type = BackupOption.Google,
                    isEnabled = false,
                    lastSuccessDate = null,
                    lastFailureDate = null,
                ),
            ),
            onBackClick = {},
            onSeedPhraseClick = {},
            onPasswordClick = {},
            onUploadNowClick = {},
            onLearnMoreClick = {},
            onBackupCheckedChange = {},
        )
    }
}