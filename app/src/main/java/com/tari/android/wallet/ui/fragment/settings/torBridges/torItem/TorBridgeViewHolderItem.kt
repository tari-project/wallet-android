package com.tari.android.wallet.ui.fragment.settings.torBridges.torItem

import com.tari.android.wallet.R
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

sealed class TorBridgeViewHolderItem(val title: String, var isSelected: Boolean = false) : CommonViewHolderItem() {
    class Empty(resourceManager: ResourceManager, isSelected: Boolean = false) :
        TorBridgeViewHolderItem(resourceManager.getString(R.string.tor_bridges_no_bridges), isSelected)

    class Bridge(val bridgeConfiguration: TorBridgeConfiguration, isSelected: Boolean) : TorBridgeViewHolderItem(bridgeConfiguration.toString(), isSelected)

    class CustomBridges(resourceManager: ResourceManager) : TorBridgeViewHolderItem(resourceManager.getString(R.string.tor_bridges_custom_bridges))

    override val viewHolderUUID: String = title
}