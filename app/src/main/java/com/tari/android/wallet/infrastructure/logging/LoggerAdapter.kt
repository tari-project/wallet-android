package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.DiskLogAdapter
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.WalletConfig

class LoggerAdapter(private val walletConfig: WalletConfig) {
    fun init() {
        Logger.addLogAdapter(AndroidLogAdapter())
        Logger.addLogAdapter(DiskLogAdapter())
        Logger.addLogAdapter(LocalFileAdapter(walletConfig.getApplicationLogsFilePath()))
        Logger.addLogAdapter(SentryLogAdapter())
    }
}