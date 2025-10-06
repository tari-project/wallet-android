package com.tari.android.wallet.ui.screen.settings.backup.backupSettings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import com.tari.android.wallet.ui.common.CommonFragment
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.composeContent

class BackupSettingsFragment : CommonFragment<BackupSettingsViewModel>() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = composeContent {
        val uiState by viewModel.uiState.collectAsState()

        TariDesignSystem(viewModel.currentTheme) {
            BackupSettingsScreen(
                uiState = uiState,
                onBackClick = { viewModel.onBackPressed() },
                onSeedPhraseClick = { viewModel.onBackupWithRecoveryPhrase() },
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
                BackupSettingsViewModel.Effect.SetupStorage -> viewModel.setupStorage(this)
            }
        }
    }

    @Deprecated("Deprecated in Java") // TODO use the modernier way of handling this
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        fun newInstance() = BackupSettingsFragment()
    }
}