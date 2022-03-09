package com.tari.android.wallet.ui.fragment.settings.torBridges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.data.sharedPrefs.tor.TorSharedRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.settings.torBridges.torItem.TorBridgeViewHolderItem
import javax.inject.Inject

class TorBridgesSelectionViewModel() : CommonViewModel() {

    @Inject
    lateinit var torSharedRepository: TorSharedRepository

    @Inject
    internal lateinit var walletManager: WalletManager

    init {
        component.inject(this)
    }

    private val _torBridges = MutableLiveData<MutableList<TorBridgeViewHolderItem>>()
    val torBridges: LiveData<MutableList<TorBridgeViewHolderItem>> = _torBridges

    private val _navigation = SingleLiveEvent<TorBridgeNavigation>()
    val navigation: LiveData<TorBridgeNavigation> = _navigation

    init {
        loadData()
    }

    fun loadData() {
        val noBridges = TorBridgeViewHolderItem.Empty(resourceManager)
        val customBridges = TorBridgeViewHolderItem.CustomBridges(resourceManager)

        val bridgeConfigurations = mutableListOf<TorBridgeViewHolderItem>()
        bridgeConfigurations.add(noBridges)

        val bridges = mutableListOf<TorBridgeViewHolderItem.Bridge>()
        bridges.addAll(torSharedRepository.customTorBridges.orEmpty().map { TorBridgeViewHolderItem.Bridge(it, false) })
        bridges.firstOrNull { it.bridgeConfiguration == torSharedRepository.currentTorBridge }?.isSelected = true

        bridgeConfigurations.addAll(bridges)
        noBridges.isSelected = bridges.isEmpty()

        bridgeConfigurations.add(customBridges)

        _torBridges.postValue(bridgeConfigurations)
    }

    fun select(torBridgeItem: TorBridgeViewHolderItem) {
        when (torBridgeItem) {
            is TorBridgeViewHolderItem.Empty -> connectNoBridges(torBridgeItem)
            is TorBridgeViewHolderItem.Bridge -> connectBridge(torBridgeItem)
            is TorBridgeViewHolderItem.CustomBridges -> _navigation.postValue(TorBridgeNavigation.ToCustomBridges)
        }
        _torBridges.postValue(_torBridges.value)
    }

    private fun connectNoBridges(torBridgeItem: TorBridgeViewHolderItem.Empty) {
        selectItem(torBridgeItem)
        torSharedRepository.currentTorBridge = null
        restartTor()
    }

    private fun connectBridge(torBridgeItem: TorBridgeViewHolderItem.Bridge) {
        selectItem(torBridgeItem)
        torSharedRepository.currentTorBridge = torBridgeItem.bridgeConfiguration
        restartTor()
    }

    private fun selectItem(torBridgeItem: TorBridgeViewHolderItem) {
        _torBridges.value.orEmpty().forEach { it.isSelected = false }
        torBridgeItem.isSelected = true
        _backPressed.call()
    }

    private fun restartTor() {
        //todo need to test closely
        walletManager.stop()
        walletManager.start()
    }
}