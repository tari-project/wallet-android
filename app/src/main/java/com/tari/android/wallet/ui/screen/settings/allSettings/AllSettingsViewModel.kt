package com.tari.android.wallet.ui.screen.settings.allSettings

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class AllSettingsViewModel : CommonSettingsViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        UiState(
            versionText = networkRepository.versionInfo,
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onVersionClick() {
        copyToClipboard(
            clipLabel = resourceManager.getString(R.string.all_settings_version_text_copy_title),
            clipText = uiState.value.versionText,
            toastMessage = resourceManager.getString(R.string.all_settings_version_text_copy_toast_message),
        )
    }

    data class UiState(
        val versionText: String,
    )
}
