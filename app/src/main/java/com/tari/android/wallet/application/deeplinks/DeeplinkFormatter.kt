package com.tari.android.wallet.application.deeplinks

import android.net.Uri
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository

class DeeplinkFormatter(private val networkRepository: NetworkRepository) {
    fun parse(deepLink: String): DeepLink? {
        val uri = Uri.parse(Uri.decode(deepLink))

        if (!uri.authority.equals(networkRepository.currentNetwork!!.network.uriComponent)) {
            return null
        }

        var paramentrs = uri.queryParameterNames.associateWith { uri.getQueryParameter(it).orEmpty() }.toMutableMap()
        val command = uri.path.orEmpty().trimStart('/')
        if (command == DeepLink.Contacts.contactsCommand) {
            val values = uri.query.orEmpty().split("&").map {
                val (key, value) = it.split("=")
                key to value
            }.toMap()
            paramentrs = values.toMutableMap()
        }
        return DeepLink.getByCommand(command, paramentrs)
    }

    fun toDeeplink(deepLink: DeepLink): String {
        val builder = Uri.Builder()
            .scheme("tari")
            .authority(networkRepository.currentNetwork!!.network.uriComponent)
            .appendPath(deepLink.getCommand())

        deepLink.getParams().forEach { (key, value) ->
            builder.appendQueryParameter(key, value)
        }

        return Uri.encode(builder.build().toString())
    }

    companion object {
        const val scheme = "tari"
    }
}