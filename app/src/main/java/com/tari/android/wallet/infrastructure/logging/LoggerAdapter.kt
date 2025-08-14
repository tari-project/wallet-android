package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoggerAdapter @Inject constructor(
    private val walletConfig: WalletConfig,
    private val walletManager: WalletManager,
    private val sentryPrefRepository: SentryPrefRepository,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun init() {
        val formatStrategy: FormatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false) // (Optional) Whether to show thread info or not. Default true
            .methodCount(2) // (Optional) How many method line to show. Default 2
            .methodOffset(5) // (Optional) Hides internal method calls up to offset. Default 5
            .tag("") // (Optional) Global tag for every log. Default PRETTY_LOGGER
            .build()

        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        Logger.addLogAdapter(FFIFileAdapter(walletManager.walletInstance))
        Logger.addLogAdapter(SentryLogAdapter(walletConfig, sentryPrefRepository, applicationScope))
    }
}