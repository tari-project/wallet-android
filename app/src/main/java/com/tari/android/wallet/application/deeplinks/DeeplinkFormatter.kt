package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository

class DeeplinkFormatter(private val networkRepository: NetworkRepository) {
    fun parse(deepLink: String): DeepLink? {
        val uri = Uri.parse(deepLink)
        if (!uri.scheme.equals(scheme, true)) {
            return null
        }

        if (!uri.authority.equals(networkRepository.currentNetwork!!.network.uriComponent)) {
            return null
        }

        return DeepLink.getByCommand(uri.path.orEmpty().trimStart('/'), uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() })
    }

    fun toDeeplink(deepLink: DeepLink): String {
        val builder = Uri.Builder()
            .scheme("tari")
            .authority(networkRepository.currentNetwork!!.network.uriComponent)
            .appendPath(deepLink.getCommand())

        deepLink.getParams().forEach { (key, value) ->
            builder.appendQueryParameter(key, value)
        }

        return Uri.decode(builder.build().toString())
    }

    companion object {
        const val scheme = "tari"
    }
}