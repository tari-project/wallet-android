package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.WalletConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggerAdapter @Inject constructor(val walletConfig: WalletConfig) {
    fun init() {
        Logger.addLogAdapter(AndroidLogAdapter())
        Logger.addLogAdapter(FFIFileAdapter())
        @Suppress("KotlinConstantConditions")
        if (BuildConfig.FLAVOR != "privacy") {
            Logger.addLogAdapter(SentryLogAdapter(walletConfig))
        }
    }
}