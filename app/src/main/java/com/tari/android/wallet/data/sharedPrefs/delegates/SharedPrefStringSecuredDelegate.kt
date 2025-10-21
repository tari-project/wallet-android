package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.Context
import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.PrefUpdater
import de.adorsys.android.securestoragelibrary.SecurePreferences
import kotlin.reflect.KProperty

class SharedPrefStringSecuredDelegate(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val prefsUpdater: PrefUpdater,
    private val name: String,
    private val defValue: String? = null,
) {
    init {
        prefsUpdater.notifySettingsUpdated()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = SecurePreferences.getStringValue(name, context, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        prefs.edit().apply {
            if (value == null) SecurePreferences.removeValue(name, context)
            else SecurePreferences.setValue(name, value, context)
        }.apply()
        prefsUpdater.notifySettingsUpdated()
    }
}