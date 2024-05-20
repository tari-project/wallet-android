package com.tari.android.wallet.application.deeplinks

import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class DeeplinkHandler @Inject constructor(networkRepository: NetworkPrefRepository) {

    private val deeplinkFormatter = DeeplinkFormatter(networkRepository)

    fun handle(deepLink: String): DeepLink? = deeplinkFormatter.parse(deepLink)

    fun getDeeplink(deeplink: DeepLink): String = deeplinkFormatter.toDeeplink(deeplink)
}