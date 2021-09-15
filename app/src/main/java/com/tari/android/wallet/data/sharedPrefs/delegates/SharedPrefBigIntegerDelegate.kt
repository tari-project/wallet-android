package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import java.math.BigInteger
import kotlin.reflect.KProperty

class SharedPrefBigIntegerDelegate(
    val prefs: SharedPreferences,
    val name: String,
    val defValue: BigInteger? = null
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): BigInteger? = prefs.getString(name, defValue?.toString())?.run(::BigInteger)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: BigInteger?) =
        prefs.edit().run {
            putString(name, value?.toString())
            apply()
        }
}

