package com.tari.android.wallet.ui.fragment.settings.allSettings.row

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class SettingsRowViewDto(
    val title: String,
    val leftIconId: Int? = null,
    val iconId: Int? = null,
    val warning: Boolean = false,
    val style: SettingsRowStyle = SettingsRowStyle.Normal,
    val action: () -> Unit,
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = title
}