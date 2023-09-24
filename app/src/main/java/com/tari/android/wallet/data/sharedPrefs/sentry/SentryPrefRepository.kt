package com.tari.android.wallet.data.sharedPrefs.sentry

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryPrefRepository @Inject constructor(sharedPrefs: SharedPreferences, networkRepository: NetworkRepository) :
    CommonRepository(networkRepository) {

    private object Key {
        const val disabledTimestamps = "tari_sentry_disabled"
    }

    var isEnabled: Boolean? by SharedPrefBooleanNullableDelegate(
        sharedPrefs,
        this,
        formatKey(Key.disabledTimestamps),
    )

    fun clear() {
        isEnabled = null
    }
}
