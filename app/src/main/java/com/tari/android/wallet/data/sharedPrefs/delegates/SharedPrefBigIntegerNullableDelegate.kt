package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import com.tari.android.wallet.data.sharedPrefs.CommonPrefRepository
import java.math.BigInteger
import kotlin.reflect.KProperty

class SharedPrefBigIntegerNullableDelegate(
    val prefs: SharedPreferences,
    val commonRepository: CommonPrefRepository,
    val name: String,
    val defValue: BigInteger? = null
) {
    init {
        commonRepository.updateNotifier.onNext(Unit)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): BigInteger? = prefs.getString(name, defValue?.toString())?.run(::BigInteger)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: BigInteger?) =
        prefs.edit().run {
            putString(name, value?.toString())
            apply()
            commonRepository.updateNotifier.onNext(Unit)
        }
}

