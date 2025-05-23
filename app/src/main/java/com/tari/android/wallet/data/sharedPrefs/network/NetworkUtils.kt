package com.tari.android.wallet.data.sharedPrefs.network

import com.google.gson.Gson
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository

fun NetworkPrefRepository.formatKey(key: String): String {
    val catching = runCatching { key + "_" + this.currentNetwork.network.displayName }
    if (catching.isSuccess) {
        return catching.getOrNull().orEmpty()
    } else {
        try {
            val networkGson = Gson().toJson(this.currentNetwork, TariNetwork::class.java)
            throw NoSupportedNetworkException(key + networkGson)
        } catch (e: Throwable) {
            throw NoSupportedNetworkException(key + e.message)
        }
    }
}

fun CommonPrefRepository.formatKey(key: String): String = this.networkRepository.formatKey(key)