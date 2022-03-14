package com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.application.WalletManager
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.data.sharedPrefs.tor.TorSharedRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.dialog.error.ErrorDialogArgs
import javax.inject.Inject

class CustomTorBridgesViewModel() : CommonViewModel() {

    @Inject
    lateinit var torSharedRepository: TorSharedRepository

    @Inject
    internal lateinit var walletManager: WalletManager

    init {
        component.inject(this)
    }

    private val _navigation = MutableLiveData<CustomBridgeNavigation>()
    val navigation: LiveData<CustomBridgeNavigation> = _navigation

    fun openRequestPage() = _openLink.postValue(resourceManager.getString(R.string.tor_bridges_url))

    fun navigateToScanQr() = _navigation.postValue(CustomBridgeNavigation.ScanQrCode)

    fun navigateToUploadQr() = _navigation.postValue(CustomBridgeNavigation.UploadQrCode)

    fun connect(inputStr: String) {
        val newBridges = mutableListOf<TorBridgeConfiguration>()
        for (bridgeInput in inputStr.split("\n").filter { it.isNotEmpty() }) {
            val splitted = bridgeInput.split(" ").filter { it.isNotEmpty() }.toMutableList()
            var technology = ""
            if (splitted.size == 3 || splitted.size == 5) {
                technology = splitted[0]
                splitted.removeAt(0)
            }
            if (splitted.size == 2 || splitted.size == 4) {
                val ipAndPort = splitted[0].split(":")
                if (ipAndPort.size != 2) {
                    incorrectFormat()
                    return
                }
                if (splitted.size == 2) {
                    newBridges.add(TorBridgeConfiguration(technology, ipAndPort[0], ipAndPort[1], splitted.last()))
                } else {
                    val cert = splitted[2].replaceFirst("cert=", "", true)
                    val mode = splitted[3].replaceFirst("iat-mode=", "", true)
                    newBridges.add(TorBridgeConfiguration(technology, ipAndPort[0], ipAndPort[1], splitted.last(), cert, mode))
                }
            } else {
                incorrectFormat()
                return
            }
        }
        if (newBridges.size == 0) {
            incorrectFormat()
            return
        }

        newBridges.forEach { torSharedRepository.addTorBridgeConfiguration(it) }
        _backPressed.postValue(Unit)
    }

    private fun incorrectFormat() {
        val args = ErrorDialogArgs(
            resourceManager.getString(R.string.common_error_title),
            resourceManager.getString(R.string.tor_bridges_incorrect_format)
        )
        _errorDialog.postValue(args)
    }
}