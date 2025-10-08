package com.tari.android.wallet.data.sharedPrefs.yat

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.EmojiId
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YatPrefRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    private object Key {
        const val YAT = "tari_wallet_yat_string"
    }

    var connectedYat: EmojiId? by SharedPrefStringDelegate(
        prefs = sharedPreferences,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.YAT),
    )

    fun saveYat(newYat: EmojiId?) {
        connectedYat = newYat
    }

    fun clear() {
        connectedYat = null
    }
}