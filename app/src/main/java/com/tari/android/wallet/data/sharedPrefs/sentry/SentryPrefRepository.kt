package com.tari.android.wallet.data.sharedPrefs.sentry

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryPrefRepository @Inject constructor(sharedPrefs: SharedPreferences, networkRepository: NetworkPrefRepository) :
    CommonPrefRepository(networkRepository) {

    private object Key {
        const val DISABLED_TIMESTAMPS = "tari_sentry_disabled"
    }

    var isEnabled: Boolean? by SharedPrefBooleanNullableDelegate(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(Key.DISABLED_TIMESTAMPS),
    )

    fun clear() {
        isEnabled = null
    }
}
