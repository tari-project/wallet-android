package com.tari.android.wallet.ui.screen.settings.allSettings.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.tari_about_icon_text_about
import com.tari.android.wallet.R.string.tari_about_icon_text_bullhorn
import com.tari.android.wallet.R.string.tari_about_icon_text_cloud
import com.tari.android.wallet.R.string.tari_about_icon_text_delete
import com.tari.android.wallet.R.string.tari_about_icon_text_final
import com.tari.android.wallet.R.string.tari_about_icon_text_keyboard
import com.tari.android.wallet.R.string.tari_about_icon_text_locked
import com.tari.android.wallet.R.string.tari_about_icon_text_magnifier
import com.tari.android.wallet.R.string.tari_about_icon_text_networking
import com.tari.android.wallet.R.string.tari_about_icon_text_password
import com.tari.android.wallet.R.string.tari_about_icon_text_privacy
import com.tari.android.wallet.R.string.tari_about_icon_text_refresh
import com.tari.android.wallet.R.string.tari_about_icon_text_repair
import com.tari.android.wallet.R.string.tari_about_icon_text_seed_words
import com.tari.android.wallet.R.string.tari_about_icon_text_select_theme
import com.tari.android.wallet.R.string.tari_about_icon_text_server
import com.tari.android.wallet.R.string.tari_about_icon_text_speach_bublles
import com.tari.android.wallet.R.string.tari_about_icon_text_writing
import com.tari.android.wallet.R.string.tari_about_icon_url_about
import com.tari.android.wallet.R.string.tari_about_icon_url_bullhorn
import com.tari.android.wallet.R.string.tari_about_icon_url_cloud
import com.tari.android.wallet.R.string.tari_about_icon_url_delete
import com.tari.android.wallet.R.string.tari_about_icon_url_final
import com.tari.android.wallet.R.string.tari_about_icon_url_keyboard
import com.tari.android.wallet.R.string.tari_about_icon_url_locked
import com.tari.android.wallet.R.string.tari_about_icon_url_magnifier
import com.tari.android.wallet.R.string.tari_about_icon_url_networking
import com.tari.android.wallet.R.string.tari_about_icon_url_password
import com.tari.android.wallet.R.string.tari_about_icon_url_privacy
import com.tari.android.wallet.R.string.tari_about_icon_url_refresh
import com.tari.android.wallet.R.string.tari_about_icon_url_repair
import com.tari.android.wallet.R.string.tari_about_icon_url_seed_words
import com.tari.android.wallet.R.string.tari_about_icon_url_select_theme
import com.tari.android.wallet.R.string.tari_about_icon_url_server
import com.tari.android.wallet.R.string.tari_about_icon_url_speach_bublles
import com.tari.android.wallet.R.string.tari_about_icon_url_writing
import com.tari.android.wallet.R.string.tari_about_license_url
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.settings.allSettings.about.list.TariIconViewHolderItem

class TariAboutViewModel : CommonViewModel() {

    private val _iconList = MutableLiveData<MutableList<TariIconViewHolderItem>>()
    val iconList: LiveData<MutableList<TariIconViewHolderItem>> = _iconList

    init {
        component.inject(this)
        initList()
    }

    private fun initList() {
        _iconList.value = mutableListOf(
            TariIconViewHolderItem(R.drawable.vector_all_settings_backup_options_icon, tari_about_icon_text_locked, tari_about_icon_url_locked),
            TariIconViewHolderItem(R.drawable.vector_all_settings_about_icon, tari_about_icon_text_about, tari_about_icon_url_about),
            TariIconViewHolderItem(R.drawable.vector_all_settings_report_bug_icon, tari_about_icon_text_speach_bublles, tari_about_icon_url_speach_bublles),
            TariIconViewHolderItem(R.drawable.vector_all_settings_contribute_to_tari_icon, tari_about_icon_text_keyboard, tari_about_icon_url_keyboard),
            TariIconViewHolderItem(R.drawable.vector_all_settings_user_agreement_icon, tari_about_icon_text_writing, tari_about_icon_url_writing),
            TariIconViewHolderItem(R.drawable.vector_all_settings_privacy_policy_icon, tari_about_icon_text_privacy, tari_about_icon_url_privacy),
            TariIconViewHolderItem(R.drawable.vector_all_settings_disclaimer_icon, tari_about_icon_text_bullhorn, tari_about_icon_url_bullhorn),
            TariIconViewHolderItem(R.drawable.vector_all_settings_select_theme_icon, tari_about_icon_text_select_theme, tari_about_icon_url_select_theme),
            TariIconViewHolderItem(R.drawable.vector_all_settings_background_service_icon, tari_about_icon_text_refresh, tari_about_icon_url_refresh),
            TariIconViewHolderItem(R.drawable.vector_all_settings_block_explorer_icon, tari_about_icon_text_magnifier, tari_about_icon_url_magnifier),
            TariIconViewHolderItem(R.drawable.vector_all_settings_bridge_configuration_icon, tari_about_icon_text_repair, tari_about_icon_url_repair),
            TariIconViewHolderItem(R.drawable.vector_all_settings_select_network_icon, tari_about_icon_text_server, tari_about_icon_url_server),
            TariIconViewHolderItem(R.drawable.vector_all_settings_select_base_node_icon, tari_about_icon_text_networking, tari_about_icon_url_networking),
            TariIconViewHolderItem(R.drawable.vector_all_settings_delete_button_icon, tari_about_icon_text_delete, tari_about_icon_url_delete),
            TariIconViewHolderItem(R.drawable.vector_backup_onboarding_seed_words, tari_about_icon_text_seed_words, tari_about_icon_url_seed_words),
            TariIconViewHolderItem(R.drawable.vector_backup_onboarding_cloud, tari_about_icon_text_cloud, tari_about_icon_url_cloud),
            TariIconViewHolderItem(R.drawable.vector_backup_onboarding_password, tari_about_icon_text_password, tari_about_icon_url_password),
            TariIconViewHolderItem(R.drawable.vector_backup_onboarding_final, tari_about_icon_text_final, tari_about_icon_url_final),
            TariIconViewHolderItem(R.drawable.vector_contact_action_link, tari_about_icon_text_final, tari_about_icon_url_final),
            TariIconViewHolderItem(R.drawable.vector_contact_action_unlink, tari_about_icon_text_final, tari_about_icon_url_final),
            TariIconViewHolderItem(R.drawable.vector_contact_book_icon, tari_about_icon_text_final, tari_about_icon_url_final),
            TariIconViewHolderItem(R.drawable.vector_share_bluetooth, tari_about_icon_text_final, tari_about_icon_url_final),
            TariIconViewHolderItem(R.drawable.vector_share_nfc, tari_about_icon_text_final, tari_about_icon_url_final),
            TariIconViewHolderItem(R.drawable.vector_empty_wallet, tari_about_icon_text_final, tari_about_icon_url_final),
        )
    }

    fun openAboutUrl(item: TariIconViewHolderItem) {
        openUrl(resourceManager.getString(item.iconLink))
    }

    fun openLicense() {
        openUrl(resourceManager.getString(tari_about_license_url))
    }
}