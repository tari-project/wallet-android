package com.tari.android.wallet.ui.screen.settings.networkSelection

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.application.walletManager.doOnWalletNotReady
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import com.tari.android.wallet.di.DiContainer
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.dialog.confirm.ConfirmDialogArgs
import com.tari.android.wallet.ui.screen.settings.networkSelection.networkItem.NetworkViewHolderItem
import com.tari.android.wallet.util.extension.launchOnIo
import javax.inject.Inject

class NetworkSelectionViewModel : CommonViewModel() {

    @Inject
    lateinit var walletConfig: WalletConfig

    private val _networks = MutableLiveData<List<CommonViewHolderItem>>()
    val networks: LiveData<List<CommonViewHolderItem>> = _networks

    private val _recreate = SingleLiveEvent<Unit>()
    val recreate: LiveData<Unit> = _recreate

    init {
        component.inject(this)
        loadData()
    }

    private fun loadData() {
        val networks = networkRepository.supportedNetworks
        val currentNetwork = networkRepository.currentNetwork.network
        _networks.postValue(networks.map { NetworkViewHolderItem(it, currentNetwork) })
    }

    fun selectNetwork(networkViewHolderItem: NetworkViewHolderItem) {
        if (networkViewHolderItem.network.network == networkRepository.currentNetwork.network) {
            onBackPressed()
            return
        }

        if (walletConfig.walletExists()) {
            showModularDialog(
                ConfirmDialogArgs(
                    title = resourceManager.getString(R.string.all_settings_select_network_confirm_title),
                    description = resourceManager.getString(R.string.all_settings_select_network_confirm_description),
                    onConfirm = { changeNetwork(networkViewHolderItem.network) },
                ).getModular(resourceManager)
            )
        } else {
            changeNetwork(networkViewHolderItem.network)
        }
    }

    private fun changeNetwork(newNetwork: TariNetwork) {
        networkRepository.currentNetwork = newNetwork
        loadData()

        launchOnIo {
            walletManager.doOnWalletNotReady {
                DiContainer.reInitContainer()
                _recreate.postValue(Unit)
            }
        }

        walletManager.stop()
    }
}