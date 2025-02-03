/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.notification

import android.app.Notification
import android.app.Notification.DEFAULT_ALL
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class NotificationHelper @Inject constructor(private val context: Context) {

    companion object {
        // notification channel id
        private const val SERVICE_NOTIFICATION_CHANNEL_ID = "com.tari.android.wallet.service.WALLET_SERVICE_NOTIFICATION"
        private const val APP_NOTIFICATION_CHANNEL_ID = "com.tari.android.wallet.WALLET_NOTIFICATION"
        private const val APP_NOTIFICATION_GROUP_ID = 1000
        private const val APP_NOTIFICATION_GROUP_NAME = "com.tari.android.wallet.notification.TX"
    }

    private var notificationManager = NotificationManagerCompat.from(context)
    private val logger
        get() = Logger.t(NotificationHelper::class.simpleName)

    fun createNotificationChannels() {
        // service notification channel
        val serviceNotificationChannel = NotificationChannel(
            SERVICE_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.wallet_service_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            setSound(null, null)
            setShowBadge(false)
            description = ""
        }
        notificationManager.createNotificationChannel(serviceNotificationChannel)
        // app notification channel
        val appNotificationChannel = NotificationChannel(
            APP_NOTIFICATION_CHANNEL_ID,
            context.getString(R.string.app_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setShowBadge(false)
            description = ""
            notificationManager.createNotificationChannel(this)
            importance = NotificationManager.IMPORTANCE_HIGH
        }
        notificationManager.createNotificationChannel(appNotificationChannel)
        logger.i("Channels was created")
    }

    private val txGroupNotification: Notification = NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_ID).run {
        setGroupSummary(true)
        setSmallIcon(R.drawable.vector_icon_send_tari)
        setGroup(APP_NOTIFICATION_GROUP_NAME)
        setAutoCancel(true)
        setGroupSummary(true)
        build()
    }

    /**
     * Posts standard Android heads-up notification.
     */
    fun postNotification(title: String, body: String) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return

        // prepare notification
        val notification = NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_ID).run {
            setContentTitle(title)
            setContentText(body)
            setSmallIcon(R.drawable.vector_notification_icon)
            setDefaults(DEFAULT_ALL)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setCategory(NotificationCompat.CATEGORY_EVENT)
            priority = NotificationCompat.PRIORITY_MAX
            build()
        }

        try {
            notificationManager.notify(APP_NOTIFICATION_GROUP_ID, txGroupNotification)
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
