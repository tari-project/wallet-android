package com.tari.android.wallet.ui.screen.settings.allSettings.support

import com.tari.android.wallet.ui.screen.settings.allSettings.CommonSettingsViewModel
import com.tari.android.wallet.util.DebugConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class SupportSettingsViewModel : CommonSettingsViewModel() {

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            showTtlStore = DebugConfig.showTtlStoreMenu,
            showBlockExplorer = networkRepository.currentNetwork.isBlockExplorerAvailable,
        )
    )
    val uiState = _uiState.asStateFlow()

    data class UiState(
        val showBlockExplorer: Boolean,
        val showTtlStore: Boolean,
    )
}