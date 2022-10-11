package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.SentryLevel
import io.sentry.protocol.Message

class SentryLogAdapter : LogAdapter {

    override fun isLoggable(priority: Int, tag: String?): Boolean = true

    override fun log(priority: Int, tag: String?, message: String) {
        if (priority == Logger.ERROR) {
            Sentry.captureException(SentryException(message), tag)
        } else {
            val event = SentryEvent().apply {
                this.message = Message().apply {
                    this.message = message
                }
                this.setTag(tag.orEmpty(), tag.orEmpty())
                this.level = when (priority) {
                    Logger.ERROR -> SentryLevel.ERROR
                    Logger.WARN -> SentryLevel.WARNING
                    Logger.DEBUG -> SentryLevel.DEBUG
                    else -> SentryLevel.INFO
                }
            }
            Sentry.captureEvent(event)
        }
    }
}