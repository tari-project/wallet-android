package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import kotlin.reflect.KProperty

class SharedPrefStringDelegate(
    val prefs: SharedPreferences,
    val commonRepository: CommonPrefRepository,
    val name: String,
    val defValue: String? = null
) {
    init {
        commonRepository.updateNotifier.onNext(Unit)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String? = prefs.getString(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String?) =
        prefs.edit().run {
            putString(name, value)
            apply()
            commonRepository.updateNotifier.onNext(Unit)
        }
}

