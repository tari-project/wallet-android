package com.tari.android.wallet.ui.screen.settings.allSettings.backupOptions

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.screen.settings.allSettings.PresentationBackupState

class SettingsBackupOptionViewHolderItem(
    var backupState: PresentationBackupState? = null,
    val leftIconId: Int,
    val action: () -> Unit
) : CommonViewHolderItem() {
    override val viewHolderUUID: String = backupState.toString()
}