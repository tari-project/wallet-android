package com.tari.android.wallet.ui.fragment.settings.torBridges.customBridges

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.common.CommonViewModel

class CustomTorBridgesViewModel() : CommonViewModel() {

    private val _navigation = MutableLiveData<CustomBridgeNavigation>()
    val navigation: LiveData<CustomBridgeNavigation> = _navigation

    fun openRequestPage() =  _openLink.postValue(resourceManager.getString(R.string.tor_bridges_url))

    fun navigateToScanQr() = _navigation.postValue(CustomBridgeNavigation.ScanQrCode)

    fun navigateToUploadQr() = _navigation.postValue(CustomBridgeNavigation.UploadQrCode)
}