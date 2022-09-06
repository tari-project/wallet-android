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

class CustomTorBridgesViewModel : CommonViewModel() {

    @Inject
    lateinit var torSharedRepository: TorSharedRepository

    @Inject
    lateinit var walletManager: WalletManager

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
            val splitInput = bridgeInput.split(" ").filter { it.isNotEmpty() }.toMutableList()
            var technology = ""
            if (splitInput.size == 3 || splitInput.size == 5) {
                technology = splitInput[0]
                splitInput.removeAt(0)
            }
            if (splitInput.size == 2 || splitInput.size == 4) {
                val ipAndPort = splitInput[0].split(":")
                if (ipAndPort.size != 2) {
                    incorrectFormat()
                    return
                }
                if (splitInput.size == 2) {
                    newBridges.add(TorBridgeConfiguration(technology, ipAndPort[0], ipAndPort[1], splitInput.last()))
                } else {
                    val fingerprint = splitInput[1]
                    val cert = splitInput[2].replaceFirst("cert=", "", true)
                    val mode = splitInput[3].replaceFirst("iat-mode=", "", true)
                    newBridges.add(TorBridgeConfiguration(technology, ipAndPort[0], ipAndPort[1], fingerprint, cert, mode))
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
        _modularDialog.postValue(args.getModular(resourceManager))
    }
}