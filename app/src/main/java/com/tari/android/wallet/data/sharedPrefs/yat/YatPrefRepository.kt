package com.tari.android.wallet.data.sharedPrefs.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import com.tari.android.wallet.util.EmojiId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YatPrefRepository @Inject constructor(
    sharedPreferences: SharedPreferences,
    networkRepository: NetworkPrefRepository,
) : CommonPrefRepository(networkRepository) {

    private object Key {
        const val YAT = "tari_wallet_yat_string"
    }

    var connectedYat: EmojiId? by SharedPrefStringDelegate(sharedPreferences, this, formatKey(Key.YAT))

    fun saveYat(newYat: EmojiId?) {
        connectedYat = newYat
    }

    fun clear() {
        connectedYat = null
    }
}