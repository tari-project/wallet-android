package com.tari.android.wallet.data.sharedPrefs.network

import com.tari.android.wallet.data.repository.CommonRepository

fun NetworkRepository.formatKey(key: String): String {
    val catching = runCatching { key + "_" + this.currentNetwork!!.network.displayName }
    if (catching.isSuccess) {
        return catching.getOrNull().orEmpty()
    } else {
        throw NoSupportedNetworkException()
    }
}

fun CommonRepository.formatKey(key: String): String = this.networkRepository.formatKey(key)