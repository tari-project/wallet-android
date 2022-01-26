package com.tari.android.wallet.ui.fragment.send.requestTari

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.WalletUtil
import javax.inject.Inject

class RequestTariViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: SharedPrefsRepository

    @Inject
    lateinit var networkRepository: NetworkRepository

    init {
        component.inject(this)
    }

    fun getDeepLink(amount: MicroTari) : String {
        val hex = sharedPrefsWrapper.publicKeyHexString!!
        val network = networkRepository.currentNetwork!!.network
        return WalletUtil.generateFullQrCodeDeepLink(hex, network, amount)
    }
}