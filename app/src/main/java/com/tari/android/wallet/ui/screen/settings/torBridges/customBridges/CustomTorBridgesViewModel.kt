package com.tari.android.wallet.ui.screen.settings.torBridges.customBridges

import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.data.sharedPrefs.tor.TorBridgeConfiguration
import com.tari.android.wallet.data.sharedPrefs.tor.TorPrefRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import javax.inject.Inject

class CustomTorBridgesViewModel : CommonViewModel() {

    @Inject
    lateinit var torSharedRepository: TorPrefRepository

    var text = SingleLiveEvent<String>()

    init {
        component.inject(this)
    }

    fun openRequestPage() {
        _openLink.postValue(resourceManager.getString(R.string.tor_bridges_url))
    }

    fun navigateToUploadQr() {
        tariNavigator.navigate(Navigation.CustomBridge.UploadQrCode)
    }

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
        backPressed.postValue(Unit)
    }

    fun handleQrCode(deeplink: DeepLink) {
        if (deeplink is DeepLink.TorBridges) {
            val text = deeplinkManager.getDeeplinkString(deeplink)
            this.text.postValue(text)
        }
    }

    private fun incorrectFormat() {
        showSimpleDialog(
            title = resourceManager.getString(R.string.common_error_title),
            description = resourceManager.getString(R.string.tor_bridges_incorrect_format),
        )
    }
}