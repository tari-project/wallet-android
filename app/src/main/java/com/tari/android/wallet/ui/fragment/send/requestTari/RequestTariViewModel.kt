package com.tari.android.wallet.ui.fragment.send.requestTari

import androidx.lifecycle.LiveData
import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject

class RequestTariViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    private val _deeplink: SingleLiveEvent<String> = SingleLiveEvent()
    val deeplink: LiveData<String> = _deeplink

    init {
        component.inject(this)
    }

    fun generateQRCodeDeeplink(amount: MicroTari) {
        val hex = sharedPrefsWrapper.publicKeyHexString!!
        val network = networkRepository.currentNetwork!!.network
        val fullDeeplink = WalletUtil.generateFullQrCodeDeepLink(hex, network, amount, "Alex")
        _deeplink.postValue(fullDeeplink)
    }
}