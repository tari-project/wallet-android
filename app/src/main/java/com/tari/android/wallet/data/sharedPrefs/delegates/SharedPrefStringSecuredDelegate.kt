package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.Context
import android.content.SharedPreferences
import de.adorsys.android.securestoragelibrary.SecurePreferences
import kotlin.reflect.KProperty

class SharedPrefStringSecuredDelegate(
    private val context: Context,
    private val prefs: SharedPreferences,
    private val name: String,
    private val defValue: String? = null,
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = SecurePreferences.getStringValue(context, name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) {
        prefs.edit().apply {
            if (value == null) SecurePreferences.removeValue(context, name)
            else SecurePreferences.setValue(context, name, value)
        }.apply()
    }
}