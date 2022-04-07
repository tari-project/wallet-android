package com.tari.android.wallet.ui.fragment.settings.allSettings

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions.SettingsBackupOptionViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.button.ButtonViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.divider.SettingsDividerViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.title.SettingsTitleViewHolder
import com.tari.android.wallet.ui.fragment.settings.allSettings.version.SettingsVersionViewHolder

class AllSettingsOptionAdapter: CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = mutableListOf(
        ButtonViewHolder.getBuilder(),
        SettingsTitleViewHolder.getBuilder(),
        SettingsDividerViewHolder.getBuilder(),
        SettingsVersionViewHolder.getBuilder(),
        SettingsBackupOptionViewHolder.getBuilder()
    )
}