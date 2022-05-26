package com.tari.android.wallet.ui.fragment.settings.allSettings.about

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.R.string.*
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.allSettings.about.list.TariIconViewHolderItem

class TariAboutViewModel : CommonViewModel() {

    private val _iconList = MutableLiveData<MutableList<TariIconViewHolderItem>>()
    val iconList: LiveData<MutableList<TariIconViewHolderItem>> = _iconList

    init {
        component.inject(this)
        initList()
    }

    private fun initList() {
        _iconList.value = mutableListOf(
            TariIconViewHolderItem(R.drawable.all_settings_backup_options_icon, tari_about_icon_text_locked, tari_about_icon_url_locked),
            TariIconViewHolderItem(R.drawable.all_settings_about_icon, tari_about_icon_text_about, tari_about_icon_url_about),
            TariIconViewHolderItem(R.drawable.all_settings_report_bug_icon, tari_about_icon_text_speach_bublles, tari_about_icon_url_speach_bublles),
            TariIconViewHolderItem(R.drawable.all_settings_contribute_to_tari_icon, tari_about_icon_text_keyboard, tari_about_icon_url_keyboard),
            TariIconViewHolderItem(R.drawable.all_settings_user_agreement_icon, tari_about_icon_text_writing, tari_about_icon_url_writing),
            TariIconViewHolderItem(R.drawable.all_settings_privacy_policy_icon, tari_about_icon_text_privacy, tari_about_icon_url_privacy),
            TariIconViewHolderItem(R.drawable.all_settings_disclaimer_icon, tari_about_icon_text_bullhorn, tari_about_icon_url_bullhorn),
            TariIconViewHolderItem(R.drawable.all_settings_background_service_icon, tari_about_icon_text_refresh, tari_about_icon_url_refresh),
            TariIconViewHolderItem(R.drawable.all_settings_block_explorer_icon, tari_about_icon_text_magnifier, tari_about_icon_url_magnifier),
            TariIconViewHolderItem(R.drawable.all_settings_bridge_configuration_icon, tari_about_icon_text_repair, tari_about_icon_url_repair),
            TariIconViewHolderItem(R.drawable.all_settings_select_network_icon, tari_about_icon_text_server, tari_about_icon_url_server),
            TariIconViewHolderItem(R.drawable.all_settings_select_base_node_icon, tari_about_icon_text_networking, tari_about_icon_url_networking),
            TariIconViewHolderItem(R.drawable.all_settings_delete_button_icon, tari_about_icon_text_delete, tari_about_icon_url_delete),
        )
    }

    fun openUrl(item: TariIconViewHolderItem) {
        _openLink.postValue(resourceManager.getString(item.iconLink))
    }

    fun openLicense() {
        _openLink.postValue(resourceManager.getString(tari_about_license_url))
    }
}