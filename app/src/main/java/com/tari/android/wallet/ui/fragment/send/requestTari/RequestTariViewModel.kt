package com.tari.android.wallet.ui.fragment.send.requestTari

import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.data.sharedPrefs.CorePrefRepository
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

class RequestTariViewModel : CommonViewModel() {

    @Inject
    lateinit var sharedPrefsWrapper: CorePrefRepository

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    init {
        component.inject(this)
    }

    fun getDeepLink(amount: MicroTari): String = deeplinkHandler.getDeeplink(DeepLink.Send(sharedPrefsWrapper.walletAddressBase58!!, amount))
}