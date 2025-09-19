package com.tari.android.wallet.ui.screen.settings.allSettings

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.common.recyclerView.viewHolders.SpaceVerticalViewHolder
import com.tari.android.wallet.ui.screen.settings.allSettings.row.SettingsRowViewHolder

class AllSettingsOptionAdapter : CommonAdapter<CommonViewHolderItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(
        SettingsRowViewHolder.getBuilder(),
        SpaceVerticalViewHolder.getBuilder(),
    )
}