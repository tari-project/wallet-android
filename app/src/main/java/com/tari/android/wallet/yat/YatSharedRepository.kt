package com.tari.android.wallet.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate

class YatSharedRepository(private val sharedPreferences: SharedPreferences) {
    private object Key {
        const val yatKey = "tari_wallet_yat_string"
    }

    var connectedYat : String? by SharedPrefStringDelegate(sharedPreferences, Key.yatKey)

    fun saveYat(newYat: String?) {
        connectedYat = newYat
    }
}