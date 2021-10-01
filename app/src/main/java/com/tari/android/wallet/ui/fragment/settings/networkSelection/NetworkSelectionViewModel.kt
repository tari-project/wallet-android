package com.tari.android.wallet.ui.fragment.settings.networkSelection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.data.network.NetworkRepository
import com.tari.android.wallet.data.network.TariNetwork
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.service.WalletServiceLauncher
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.fragment.settings.networkSelection.networkItem.NetworkViewHolderItem
import javax.inject.Inject

class NetworkSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var networkRepository: NetworkRepository

    @Inject
    lateinit var walletManager: WalletServiceLauncher

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
        _networks.postValue(networks.map { NetworkViewHolderItem(it, currentNetwork) }.toMutableList())
    }

    fun selectNetwork(networkViewHolderItem: NetworkViewHolderItem) {
        if (networkViewHolderItem.network.network == networkRepository.currentNetwork!!.network) {
            _backPressed.postValue(Unit)
            return
        }

        val confirmDialogArgs = ConfirmDialogArgs(
            resourceManager.getString(R.string.all_settings_select_network_confirm_title),
            resourceManager.getString(R.string.all_settings_select_network_confirm_description),
            onConfirm = { changeNetwork(networkViewHolderItem.network) }
        )
        _confirmDialog.postValue(confirmDialogArgs)
    }

    private fun changeNetwork(newNetwork: TariNetwork) {
        networkRepository.currentNetwork = newNetwork
        loadData()
        walletManager.stop()
        EventBus.clear()
        DiContainer.reinitContainer()
        _recreate.postValue(Unit)
    }
}