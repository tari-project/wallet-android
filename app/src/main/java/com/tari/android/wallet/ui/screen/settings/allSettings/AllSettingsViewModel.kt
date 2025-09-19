package com.tari.android.wallet.ui.screen.settings.allSettings

import com.tari.android.wallet.R.string.all_settings_version_text_copy_title
import com.tari.android.wallet.R.string.all_settings_version_text_copy_toast_message
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.util.extension.addTo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AllSettingsViewModel : CommonSettingsViewModel() {

    @Inject
    lateinit var backupSettingsRepository: BackupPrefRepository

    @Inject
    lateinit var settingsRepository: CorePrefRepository

    init {
        component.inject(this)
    }

    private val _allSettingsOptions = MutableStateFlow(generateOptions())
    val allSettingsOptions = _allSettingsOptions.asStateFlow()

    private val _uiState = MutableStateFlow(
        UiState(
            versionText = networkRepository.versionInfo,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        settingsRepository.updateNotifier.subscribe(
            /* onNext = */ { generateOptions() },
            /* onError = */ { logger.d("Error updating settings options", it) },
        ).addTo(compositeDisposable)
    }

    fun updateOptions() {
        _allSettingsOptions.update { generateOptions() }
    }

    private fun generateOptions(): List<CommonViewHolderItem> {
        if (!settingsRepository.walletAddressExists()) return emptyList() // Return empty list if this method called after wallet is deleted

        return listOfNotNull(

        )
    }

    fun onVersionClick() {
        copyToClipboard(
            clipLabel = resourceManager.getString(all_settings_version_text_copy_title),
            clipText = uiState.value.versionText,
            toastMessage = resourceManager.getString(all_settings_version_text_copy_toast_message),
        )
    }

    data class UiState(
        val versionText: String,
    )
}
