package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.sentry.SentryPrefRepository
import com.tari.android.wallet.util.WalletUtil
import io.sentry.Attachment
import io.sentry.Breadcrumb
import io.sentry.Hint
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SentryLogAdapter(val walletConfig: WalletConfig,
                       private val sentryPrefRepository: SentryPrefRepository) : LogAdapter {

    private var localScope = CoroutineScope(Job())

    override fun isLoggable(priority: Int, tag: String?): Boolean = sentryPrefRepository.isEnabled == true

    override fun log(priority: Int, tag: String?, message: String) {
        if (priority == Logger.ERROR) {
            localScope.launch(Dispatchers.IO) {
                try {
                    val files = WalletUtil.getLogFilesFromDirectory(walletConfig.getWalletLogFilesDirPath()).toMutableList()
                    val lines = files.firstOrNull()?.inputStream()?.bufferedReader()?.readLines()?.takeLast(100)?.joinToString("\n")

                    val breadcrumb = Breadcrumb(message).apply {
                        level = SentryLevel.INFO
                        category = tag
                        setMessage(lines.orEmpty())
                    }
                    Sentry.addBreadcrumb(breadcrumb)

                    val attachment = Hint.withAttachment(Attachment(files.firstOrNull()?.absolutePath.orEmpty()))
                    Sentry.captureEvent(SentryEvent(SentryException(message)), attachment)
                } catch (e: Throwable) {
                    Sentry.captureException(SentryException(message), Hint.withAttachment(Attachment("tag", tag.orEmpty())))
                }
            }
        } else {
            val level = when (priority) {
                Logger.ERROR -> SentryLevel.ERROR
                Logger.WARN -> SentryLevel.WARNING
                Logger.DEBUG -> SentryLevel.DEBUG
                else -> SentryLevel.INFO
            }
            val breadcrumb = Breadcrumb(message).apply {
                setLevel(level)
                category = tag
                setMessage(message)
            }

            Sentry.addBreadcrumb(breadcrumb)
        }
    }
}