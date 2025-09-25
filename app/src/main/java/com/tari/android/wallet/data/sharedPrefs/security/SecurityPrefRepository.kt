package com.tari.android.wallet.data.sharedPrefs.security

import android.content.Context
import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefBooleanNullableDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefGsonDelegate
import com.tari.android.wallet.data.sharedPrefs.delegates.SharedPrefStringSecuredDelegate
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityPrefRepository @Inject constructor(
    private val context: Context,
    private val sharedPrefs: SharedPreferences,
    private val networkRepository: NetworkPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : CommonPrefRepository(applicationScope) {

    companion object Key {
        const val IS_AUTHENTICATED = "tari_wallet_is_authenticated"
        const val IS_FEATURE_AUTHENTICATED = "tari_wallet_is_feature_authenticated"
        const val PIN_CODE = "tari_is_pincode"
        const val BIOMETRICS = "tari_is_biometrics"
        const val WALLET_DATABASE_PASSPHRASE = "tari_wallet_database_passphrase"
        const val LOGIN_ATTEMPTS = "tari_login_attempts"
    }

    var isAuthenticated: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(IS_AUTHENTICATED),
    )

    var isFeatureAuthenticated: Boolean by SharedPrefBooleanDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(IS_FEATURE_AUTHENTICATED),
    )

    var pinCode: String? by SharedPrefStringSecuredDelegate(
        context = context,
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(PIN_CODE),
        defValue = null,
    )

    var biometricsAuth: Boolean? by SharedPrefBooleanNullableDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(BIOMETRICS),
    )

    var databasePassphrase: String? by SharedPrefStringSecuredDelegate(
        context = context,
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(WALLET_DATABASE_PASSPHRASE),
    )

    var attempts: LoginAttemptList by SharedPrefGsonDelegate(
        prefs = sharedPrefs,
        prefsUpdater = this,
        name = networkRepository.currentNetwork.formatKey(LOGIN_ATTEMPTS),
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