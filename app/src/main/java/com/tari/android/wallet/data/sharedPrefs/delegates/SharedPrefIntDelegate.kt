package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import kotlin.reflect.KProperty

class SharedPrefIntDelegate(
    val prefs: SharedPreferences,
    val name: String,
    val defValue: Int = -1
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = prefs.getInt(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?) = prefs.edit().run {
        if (value == null) {
            remove(name)
        } else {
            putInt(name, value)
            apply()
        }
    }
}