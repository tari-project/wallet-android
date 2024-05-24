package com.tari.android.wallet.data.sharedPrefs.security

import android.content.Context
import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPrefRepository @Inject constructor(
    context: Context,
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkPrefRepository,
) : CommonRepository(networkRepository) {

    companion object Key {
        const val IS_AUTHENTICATED = "tari_wallet_is_authenticated"
        const val IS_FEATURE_AUTHENTICATED = "tari_wallet_is_feature_authenticated"
        const val PIN_CODE = "tari_is_pincode"
        const val BIOMETRICS = "tari_is_biometrics"
        const val WALLET_DATABASE_PASSPHRASE = "tari_wallet_database_passphrase"
        const val LOGIN_ATTEMPTS = "tari_login_attempts"
    }

    var isAuthenticated: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(IS_AUTHENTICATED))

    var isFeatureAuthenticated: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(IS_FEATURE_AUTHENTICATED))

    var pinCode: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, this, formatKey(PIN_CODE), null)

    var biometricsAuth: Boolean? by SharedPrefBooleanNullableDelegate(sharedPrefs, this, formatKey(BIOMETRICS))

    var databasePassphrase: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, this, formatKey(WALLET_DATABASE_PASSPHRASE))

    var attempts: LoginAttemptList by SharedPrefGsonDelegate<LoginAttemptList>(
        prefs = sharedPrefs,
        commonRepository = this,
        name = formatKey(LOGIN_ATTEMPTS),
        type = LoginAttemptList::class.java,
        defValue = LoginAttemptList(),
    )

    fun saveAttempt(attempt: LoginAttemptDto) {
        this.attempts = attempts.apply {
            add(attempt)
        }
        if (attempt.isSuccessful) {
            attempts = LoginAttemptList(emptyList())
        }
    }

    fun clear() {
        databasePassphrase = null
        isAuthenticated = false
        isFeatureAuthenticated = false
        pinCode = null
        biometricsAuth = null
        attempts = LoginAttemptList(emptyList())
    }
}