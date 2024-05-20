package com.tari.android.wallet.data.sharedPrefs.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YatPrefRepository @Inject constructor(
    sharedPreferences: SharedPreferences,
    networkRepository: NetworkPrefRepository,
) : CommonRepository(networkRepository) {

    private object Key {
        const val YAT = "tari_wallet_yat_string"
        const val YAT_DISCONNECTED = "tari_wallet_yat_disconnected_string"
    }

    var connectedYat: String? by SharedPrefStringDelegate(sharedPreferences, this, formatKey(Key.YAT))

    var yatWasDisconnected: Boolean by SharedPrefBooleanDelegate(sharedPreferences, this, formatKey(Key.YAT_DISCONNECTED), false)

    fun saveYat(newYat: String?) {
        connectedYat = newYat
        yatWasDisconnected = false
    }

    fun clear() {
        connectedYat = null
        yatWasDisconnected = false
    }
}