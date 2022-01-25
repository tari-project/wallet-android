package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import kotlin.reflect.KProperty

class SharedPrefBooleanNullableDelegate(
    val prefs: SharedPreferences,
    val name: String
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean? =
        if (!prefs.contains(name)) null else prefs.getBoolean(name, false)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean?) =
        prefs.edit().run {
            if (value == null) {
                remove(name)
            } else {
                putBoolean(name, value)
            }
            apply()
        }
}