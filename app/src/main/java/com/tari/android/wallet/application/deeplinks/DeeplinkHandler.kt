package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository

class DeeplinkHandler(networkRepository: NetworkRepository) {

    private val deeplinkFormatter = DeeplinkFormatter(networkRepository)

    fun handle(deepLink: String): DeepLink? = deeplinkFormatter.parse(deepLink)

    fun getDeeplink(deeplink: DeepLink): String = deeplinkFormatter.toDeeplink(deeplink)
}