package com.tari.android.wallet.data.sharedPrefs.security

import android.content.Context
import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.data.sharedPrefs.network.formatKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPrefRepository @Inject constructor(
    context: Context,
    sharedPrefs: SharedPreferences,
    networkRepository: NetworkRepository,
) : CommonRepository(networkRepository) {

    companion object Key {
        const val isAuthenticatedKey = "tari_wallet_is_authenticated"
        const val isFeatureAuthenticatedKey = "tari_wallet_is_feature_authenticated"
        const val pinCodeKey = "tari_is_pincode"
        const val biometricsKey = "tari_is_biometrics"
        const val walletDatabasePassphraseKey = "tari_wallet_database_passphrase"
        const val loginAttemptsKey = "tari_login_attempts"
    }

    var isAuthenticated: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(isAuthenticatedKey))

    var isFeatureAuthenticated: Boolean by SharedPrefBooleanDelegate(sharedPrefs, this, formatKey(isFeatureAuthenticatedKey))

    var pinCode: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, this, formatKey(pinCodeKey), null)

    var biometricsAuth: Boolean? by SharedPrefBooleanNullableDelegate(sharedPrefs, this, formatKey(biometricsKey))

    var databasePassphrase: String? by SharedPrefStringSecuredDelegate(context, sharedPrefs, this, formatKey(walletDatabasePassphraseKey))

    var attempts: LoginAttemptList? by SharedPrefGsonDelegate<LoginAttemptList>(
        sharedPrefs,
        this,
        formatKey(loginAttemptsKey),
        LoginAttemptList::class.java,
        LoginAttemptList()
    )

    fun saveAttempt(attempt: LoginAttemptDto) {
         this.attempts = attempts.orEmpty().apply {
             add(attempt)
         }
        if (attempt.isSuccessful) {
            attempts = null
        }
    }

    fun clear() {
        databasePassphrase = null
        isAuthenticated = false
        isFeatureAuthenticated = false
        pinCode = null
        biometricsAuth = null
        attempts = null
    }
}