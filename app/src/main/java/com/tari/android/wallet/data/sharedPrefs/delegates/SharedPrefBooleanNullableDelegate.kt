package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.PrefUpdater
import kotlin.reflect.KProperty

class SharedPrefBooleanNullableDelegate(
    private val prefs: SharedPreferences,
    private val prefsUpdater: PrefUpdater,
    private val name: String,
) {
    init {
        prefsUpdater.notifySettingsUpdated()
    }

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
            prefsUpdater.notifySettingsUpdated()
        }
}