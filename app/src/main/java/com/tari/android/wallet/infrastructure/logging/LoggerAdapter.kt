package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.DiskLogAdapter
import com.orhanobut.logger.Logger

class LoggerAdapter {
    fun init() {
        Logger.addLogAdapter(AndroidLogAdapter())
        Logger.addLogAdapter(DiskLogAdapter())
        Logger.addLogAdapter(SentryLogAdapter())
    }
}