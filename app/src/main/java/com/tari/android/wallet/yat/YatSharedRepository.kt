package com.tari.android.wallet.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey

class YatSharedRepository(sharedPreferences: SharedPreferences, networkRepository: NetworkRepository) : CommonRepository(networkRepository) {
    private object Key {
        const val yatKey = "tari_wallet_yat_string"
        const val yatKeyDisconnected = "tari_wallet_yat_disconnected_string"
    }

    var connectedYat: String? by SharedPrefStringDelegate(sharedPreferences, formatKey(Key.yatKey))

    var yatWasDisconnected: Boolean by SharedPrefBooleanDelegate(sharedPreferences, formatKey(Key.yatKeyDisconnected), false)

    fun saveYat(newYat: String?) {
        connectedYat = newYat
        yatWasDisconnected = false
    }

    fun clear() {
        connectedYat = null
        yatWasDisconnected = false
    }
}