package com.tari.android.wallet.infrastructure.logging

import com.orhanobut.logger.LogAdapter
import com.orhanobut.logger.Logger
import io.sentry.*

class SentryLogAdapter : LogAdapter {

    override fun isLoggable(priority: Int, tag: String?): Boolean = true

    override fun log(priority: Int, tag: String?, message: String) {
        if (priority == Logger.ERROR) {
            Sentry.captureException(SentryException(message), Hint.withAttachment(Attachment("tag", tag.orEmpty())))
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