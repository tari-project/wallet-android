package com.tari.android.wallet.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate

class YatSharedRepository(private val sharedPreferences: SharedPreferences) {
    private object Key {
        const val yatKey = "tari_wallet_yat_list"
    }

    var connectedYats : YatsList? by SharedPrefGsonDelegate(sharedPreferences,Key.yatKey,YatsList::class.java)

    fun saveYat(newYat: String) {
        val list = connectedYats ?: YatsList()
        if (!list.contains(newYat)) {
            list.add(newYat)
        }
        connectedYats = list
    }
}