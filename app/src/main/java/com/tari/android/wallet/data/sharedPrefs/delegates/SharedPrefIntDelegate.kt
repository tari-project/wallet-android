package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.repository.CommonRepository
import kotlin.reflect.KProperty

class SharedPrefIntDelegate(
    val prefs: SharedPreferences,
    val commonRepository: CommonRepository,
    val name: String,
    val defValue: Int = -1
) {
    init {
        commonRepository.updateNotifier.onNext(Unit)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): Int = prefs.getInt(name, defValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int?): Unit = prefs.edit().run {
        if (value == null) {
            remove(name)
        } else {
            putInt(name, value)
            apply()
        }
        commonRepository.updateNotifier.onNext(Unit)
    }
}