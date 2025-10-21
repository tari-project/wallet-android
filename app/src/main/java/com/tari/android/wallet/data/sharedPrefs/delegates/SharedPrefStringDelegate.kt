package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.PrefUpdater
import kotlin.reflect.KProperty

class SharedPrefStringDelegate(
    private val prefs: SharedPreferences,
    private val prefsUpdater: PrefUpdater,
    private val name: String,
    private val defValue: String? = null
) {
    init {
        prefsUpdater.notifySettingsUpdated()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = prefs.getString(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) =
        prefs.edit().run {
            putString(name, value)
            apply()
            prefsUpdater.notifySettingsUpdated()
        }
}

