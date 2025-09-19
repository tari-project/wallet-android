package com.tari.android.wallet.ui.screen.settings.allSettings

import com.tari.android.wallet.R.drawable.vector_all_settings_about_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_disclaimer_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_privacy_policy_icon
import com.tari.android.wallet.R.drawable.vector_all_settings_user_agreement_icon
import com.tari.android.wallet.R.string.all_settings_disclaimer
import com.tari.android.wallet.R.string.all_settings_privacy_policy
import com.tari.android.wallet.R.string.all_settings_user_agreement
import com.tari.android.wallet.R.string.all_settings_version_text_copy_title
import com.tari.android.wallet.R.string.all_settings_version_text_copy_toast_message
import com.tari.android.wallet.R.string.disclaimer_url
import com.tari.android.wallet.R.string.privacy_policy_url
import com.tari.android.wallet.R.string.tari_about_title
import com.tari.android.wallet.R.string.user_agreement_url
import com.tari.android.wallet.application.Navigation.AllSettings
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.data.sharedPrefs.backup.BackupPrefRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.screen.settings.allSettings.row.SettingsRowViewHolderItem
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
            SettingsRowViewHolderItem(resourceManager.getString(tari_about_title), vector_all_settings_about_icon) {
                tariNavigator.navigate(AllSettings.About)
            },
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_user_agreement), vector_all_settings_user_agreement_icon) {
                openUrl(resourceManager.getString(user_agreement_url))
            },
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_privacy_policy), vector_all_settings_privacy_policy_icon) {
                openUrl(resourceManager.getString(privacy_policy_url))
            },
            SettingsRowViewHolderItem(resourceManager.getString(all_settings_disclaimer), vector_all_settings_disclaimer_icon) {
                openUrl(resourceManager.getString(disclaimer_url))
            },
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
