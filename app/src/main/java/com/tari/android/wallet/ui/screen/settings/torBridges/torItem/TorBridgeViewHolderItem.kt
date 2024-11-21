package com.tari.android.wallet.ui.screen.settings.torBridges.torItem

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

sealed class TorBridgeViewHolderItem(val title: String, var isSelected: Boolean = false) : CommonViewHolderItem() {
    class Empty(val resourceManager: ResourceManager, isSelected: Boolean = false) :
        TorBridgeViewHolderItem(resourceManager.getString(R.string.tor_bridges_no_bridges), isSelected) {
        override fun deepCopy(): CommonViewHolderItem = Empty(resourceManager, isSelected)
    }

    class Bridge(val bridgeConfiguration: TorBridgeConfiguration, isSelected: Boolean) :
        TorBridgeViewHolderItem(bridgeConfiguration.toString(), isSelected) {
        override fun deepCopy(): CommonViewHolderItem = Bridge(bridgeConfiguration, isSelected)
    }

    class CustomBridges(resourceManager: ResourceManager) : TorBridgeViewHolderItem(resourceManager.getString(R.string.tor_bridges_custom_bridges))

    override val viewHolderUUID: String = title
}