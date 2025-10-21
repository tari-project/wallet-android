package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.composeContent

class BackupSettingsFragment : CommonFragment<BackupSettingsViewModel>() {

    private val googleSignInLauncher: ActivityResultLauncher<Intent?> = registerForActivityResult() { result ->
        viewModel.handleActivityResult(result)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            BackupSettingsScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onSeedPhraseClick = { viewModel.onBackupWithRecoveryPhraseClick() },
                onPasswordClick = { viewModel.onUpdatePassword() },
                onUploadNowClick = { viewModel.onBackupToCloud() },
                onLearnMoreClick = { viewModel.learnMore() },
                onBackupCheckedChange = { viewModel.onBackupSwitchChecked(it) },
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewModel: BackupSettingsViewModel by viewModels()
        bindViewModel(viewModel)

        collectFlow(viewModel.effect) { effect ->
            when (effect) {
                BackupSettingsViewModel.Effect.SetupStorage -> viewModel.setupStorage(googleSignInLauncher)
            }
        }
    }

    companion object {
        fun newInstance() = BackupSettingsFragment()
    }
}