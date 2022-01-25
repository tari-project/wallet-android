package com.tari.android.wallet.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository

class YatSharedRepository(private val sharedPreferences: SharedPreferences, private val networkRepository: NetworkRepository) {
    private object Key {
        const val yatKey = "tari_wallet_yat_string"
        const val yatKeyDisconnected = "tari_wallet_yat_disconnected_string"
    }

    var connectedYat : String? by SharedPrefStringDelegate(sharedPreferences, formatKey(Key.yatKey))

    var yatWasDisconnected: Boolean by SharedPrefBooleanDelegate(sharedPreferences, formatKey(Key.yatKeyDisconnected), false)

    fun saveYat(newYat: String?) {
        connectedYat = newYat
        yatWasDisconnected = false
    }

    fun clear() {
        connectedYat = null
        yatWasDisconnected = false
    }

    private fun formatKey(key: String): String = key + "_" + networkRepository.currentNetwork!!.network.displayName
}