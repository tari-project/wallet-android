package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import kotlin.reflect.KProperty

class SharedPrefBooleanDelegate(
    val prefs: SharedPreferences,
    val name: String,
    val defValue: Boolean = false
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        prefs.getBoolean(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        prefs.edit().run {
            putBoolean(name, value)
            apply()
        }
}