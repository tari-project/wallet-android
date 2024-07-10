package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import kotlin.reflect.KProperty

class SharedPrefBooleanDelegate(
    val prefs: SharedPreferences,
    val commonRepository: CommonPrefRepository,
    val name: String,
    val defValue: Boolean = false
) {
    init {
        commonRepository.updateNotifier.onNext(Unit)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Boolean =
        prefs.getBoolean(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        prefs.edit().run {
            putBoolean(name, value)
            apply()
            commonRepository.updateNotifier.onNext(Unit)
        }
}