package com.tari.android.wallet.data.sharedPrefs.sentry

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryPrefRepository @Inject constructor(
    sharedPrefs: SharedPreferences, networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    private object Key {
        const val DISABLED_TIMESTAMPS = "tari_sentry_disabled"
    }

    var isEnabled: Boolean? by SharedPrefBooleanNullableDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(Key.DISABLED_TIMESTAMPS),
    )

    fun clear() {
        isEnabled = null
    }
}
