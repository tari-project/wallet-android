package com.tari.android.wallet.ui.fragment.settings.allSettings.backupOptions

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.settings.allSettings.PresentationBackupState

class SettingsBackupOptionViewHolderItem(
    var backupState: PresentationBackupState? = null,
    var lastBackupDate: String = "",
    val leftIconId: Int,
    val action: () -> Unit
) : CommonViewHolderItem()