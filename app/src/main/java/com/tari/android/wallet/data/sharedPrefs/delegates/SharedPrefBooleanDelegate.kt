package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.PrefUpdater
import kotlin.reflect.KProperty

class SharedPrefBooleanDelegate(
    private val prefs: SharedPreferences,
    private val prefsUpdater: PrefUpdater,
    private val name: String,
    private val defValue: Boolean = false
) {
    init {
        prefsUpdater.notifySettingsUpdated()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        prefs.getBoolean(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        prefs.edit().run {
            putBoolean(name, value)
            apply()
            prefsUpdater.notifySettingsUpdated()
        }
}