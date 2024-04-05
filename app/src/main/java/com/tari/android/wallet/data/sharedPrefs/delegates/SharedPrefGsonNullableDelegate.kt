package com.tari.android.wallet.data.sharedPrefs.delegates

import android.content.SharedPreferences
import android.net.Uri
import com.google.gson.GsonBuilder
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.repository.CommonRepository
import kotlin.reflect.KProperty

class SharedPrefGsonNullableDelegate<T>(
    private val prefs: SharedPreferences,
    private val commonRepository: CommonRepository,
    private val name: String,
    private val type: Class<T>,
    private val defValue: T? = null,
) {
    private val gson = with(GsonBuilder()) {
        registerTypeAdapter(Uri::class.java, UriDeserializer())
        create()
    }

    init {
        commonRepository.updateNotifier.onNext(Unit)
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return if (prefs.contains(name)) {
            val savedValue = prefs.getString(name, "")
            try {
                gson.fromJson(savedValue, type) as T
            } catch (e: Throwable) {
                logger.i(e.toString())
                defValue
            }
        } else {
            defValue
        }
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        prefs.edit().run {
            putString(name, gson.toJson(value, type))
            apply()
            commonRepository.updateNotifier.onNext(Unit)
        }
    }

    companion object {
        private val logger
            get() = Logger.t(SharedPrefGsonNullableDelegate::class.simpleName)
    }
}