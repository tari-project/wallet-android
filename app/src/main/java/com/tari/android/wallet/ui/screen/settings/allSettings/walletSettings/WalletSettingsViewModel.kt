package com.tari.android.wallet.ui.screen.settings.allSettings.walletSettings

import com.tari.android.wallet.infrastructure.backup.BackupState
import com.tari.android.wallet.infrastructure.backup.BackupStateHandler
import com.tari.android.wallet.ui.screen.settings.allSettings.CommonSettingsViewModel
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class WalletSettingsViewModel : CommonSettingsViewModel() {

    @Inject
    lateinit var backupStateHandler: BackupStateHandler

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            backupState = backupStateHandler.backupState.value,
            pinCodeIsSet = securityPrefRepository.pinCode != null,
            screenRecordingWarning = tariSettingsSharedRepository.screenRecordingTurnedOn,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(backupStateHandler.backupState) { newState -> _uiState.update { it.copy(backupState = newState) } }

        launchOnMain {
            securityPrefRepository.doOnSettingsUpdated { _uiState.update { it.copy(pinCodeIsSet = securityPrefRepository.pinCode != null) } }
        }
        launchOnMain {
            tariSettingsSharedRepository.doOnSettingsUpdated {
                _uiState.update { it.copy(screenRecordingWarning = tariSettingsSharedRepository.screenRecordingTurnedOn) }
            }
        }
    }

    data class UiState(
        val backupState: BackupState,
        val pinCodeIsSet: Boolean,
        val screenRecordingWarning: Boolean,
    )
}