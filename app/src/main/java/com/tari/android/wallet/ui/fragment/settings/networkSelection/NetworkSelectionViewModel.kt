package com.tari.android.wallet.ui.fragment.settings.networkSelection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletState
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.fragment.settings.networkSelection.networkItem.NetworkViewHolderItem
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject

class NetworkSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var walletConfig: WalletConfig

    @Inject
    lateinit var walletServiceLauncher: WalletServiceLauncher

    private val _networks = MutableLiveData<MutableList<CommonViewHolderItem>>()
    val networks: LiveData<MutableList<CommonViewHolderItem>> = _networks

    private val _recreate = SingleLiveEvent<Unit>()
    val recreate: LiveData<Unit> = _recreate

    init {
        component.inject(this)
        loadData()
    }

    private fun loadData() {
        val networks = networkRepository.getAllNetworks()
        val currentNetwork = networkRepository.currentNetwork!!.network
        val recommendedNetworks = networkRepository.recommendedNetworks
        _networks.postValue(networks.map { NetworkViewHolderItem(it, recommendedNetworks.contains(it.network), currentNetwork) }.toMutableList())
    }

    fun selectNetwork(networkViewHolderItem: NetworkViewHolderItem) {
        if (networkViewHolderItem.network.network == networkRepository.currentNetwork!!.network) {
            _backPressed.postValue(Unit)
            return
        }

        if (WalletUtil.walletExists(walletConfig)) {
            val confirmDialogArgs = ConfirmDialogArgs(
                resourceManager.getString(R.string.all_settings_select_network_confirm_title),
                resourceManager.getString(R.string.all_settings_select_network_confirm_description),
                onConfirm = { changeNetwork(networkViewHolderItem.network) }
            )
            _modularDialog.postValue(confirmDialogArgs.getModular(resourceManager))
        } else {
            changeNetwork(networkViewHolderItem.network)
        }
    }

    private fun changeNetwork(newNetwork: TariNetwork) {
        networkRepository.currentNetwork = newNetwork
        loadData()

        EventBus.walletState.subscribe(this) {
            if (it == WalletState.NotReady) {
                EventBus.clear()
                DiContainer.reInitContainer()
                _recreate.postValue(Unit)
            }
        }

        walletServiceLauncher.stop()
    }
}