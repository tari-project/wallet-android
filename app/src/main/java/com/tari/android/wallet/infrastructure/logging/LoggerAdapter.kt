package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig

class LoggerAdapter {
    fun init() {
        Logger.addLogAdapter(AndroidLogAdapter())
        Logger.addLogAdapter(FFIFileAdapter())
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.FLAVOR != "privacy") {
            Logger.addLogAdapter(SentryLogAdapter())
        }
    }
}
