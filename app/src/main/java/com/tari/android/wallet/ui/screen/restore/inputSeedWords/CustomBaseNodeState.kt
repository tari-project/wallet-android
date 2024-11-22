package com.tari.android.wallet.ui.screen.restore.inputSeedWords

import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeDto

data class CustomBaseNodeState(
    val customBaseNode: BaseNodeDto? = null,
) {
    val isAddressSet: Boolean
        get() = customBaseNode != null
}