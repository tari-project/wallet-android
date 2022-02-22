package com.tari.android.wallet.ui.fragment.settings.torBridges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.torBridges.torItem.TorBridgeViewHolderItem

class TorBridgesSelectionViewModel() : CommonViewModel() {

    private val _torBridges = MutableLiveData<MutableList<TorBridgeViewHolderItem>>()
    val torBridges: LiveData<MutableList<TorBridgeViewHolderItem>> = _torBridges

    private val _navigation = MutableLiveData<TorBridgeNavigation>()
    val navigation: LiveData<TorBridgeNavigation> = _navigation

    init {
        val noBridges = TorBridgeViewHolderItem.Empty(resourceManager)
        val customBridges = TorBridgeViewHolderItem.CustomBridges(resourceManager)

        val bridgeConfigurations = mutableListOf<TorBridgeViewHolderItem>()
        bridgeConfigurations.add(noBridges)

        val bridges = mutableListOf<TorBridgeViewHolderItem>()
        bridgeConfigurations.addAll(bridges)
        noBridges.isSelected = bridges.isEmpty()

        bridgeConfigurations.add(customBridges)

        _torBridges.postValue(bridgeConfigurations)
    }

    fun select(torBridgeItem: TorBridgeViewHolderItem) {
        when (torBridgeItem) {
            is TorBridgeViewHolderItem.Empty -> selectItem(torBridgeItem)
            is TorBridgeViewHolderItem.Bridge -> connectBridge(torBridgeItem)
            is TorBridgeViewHolderItem.CustomBridges -> _navigation.postValue(TorBridgeNavigation.ToCustomBridges)
        }
        _torBridges.postValue(_torBridges.value)
    }

    private fun selectItem(torBridgeItem: TorBridgeViewHolderItem) {
        _torBridges.value.orEmpty().forEach { it.isSelected = false }
        torBridgeItem.isSelected = true
        _backPressed.call()
    }

    private fun connectBridge(torBridgeItem: TorBridgeViewHolderItem.Bridge) {
        selectItem(torBridgeItem)
    }
}