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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.ui.fragment.home.HomeDeeplinkScreens
import com.tari.android.wallet.util.WalletUtil

/**
 * Contains helper functions for building and posting notifications.
 *
 * @author The Tari Development Team
 */
class NotificationHelper(private val context: Context) {

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // service notification channel
            val serviceNotificationChannel = NotificationChannel(
                SERVICE_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.wallet_service_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setSound(null, null)
                setShowBadge(false)
                description = context.getString(R.string.wallet_service_description)
            }
            notificationManager.createNotificationChannel(serviceNotificationChannel)
            // app notification channel
            val appNotificationChannel = NotificationChannel(
                APP_NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.app_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(false)
                description = context.getString(R.string.wallet_service_description)
                notificationManager.createNotificationChannel(this)
                importance = NotificationManager.IMPORTANCE_HIGH
            }
            notificationManager.createNotificationChannel(appNotificationChannel)
            logger.i("Channels was created")
        }
    }

    fun buildForegroundServiceNotification(): Notification {
        val intent = Intent(context, NotificationBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        // prepare foreground service notification
        return NotificationCompat.Builder(context, SERVICE_NOTIFICATION_CHANNEL_ID).run {
            setContentTitle(context.getString(R.string.wallet_service_title))
            setContentText(context.getString(R.string.wallet_service_description))
            setContentIntent(pendingIntent)
            setSound(null)
            setSmallIcon(R.drawable.notification_icon)
            build()
        }
    }

    private val txGroupNotification: Notification = NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_ID).run {
        setGroupSummary(true)
        setSmallIcon(R.drawable.home_tx_icon)
        setGroup(APP_NOTIFICATION_GROUP_NAME)
        setAutoCancel(true)
        setGroupSummary(true)
        build()
    }

    /**
     * Posts custom-layout heads-up transaction notification.
     */
    fun postCustomLayoutTxNotification(tx: Tx) {
        logger.i("postCustomLayoutTxNotification: ${tx.id}")
        val notificationTitle = context.getString(R.string.notification_tx_received_title)
        // format spannable string
        val formattedAmount = if (tx.amount.tariValue.toDouble() % 1 == 0.toDouble()) tx.amount.tariValue.toBigInteger().toString()
        else WalletUtil.amountFormatter.format(tx.amount.tariValue)
        val notificationBody = context.getString(R.string.notification_tx_received_description_format, formattedAmount)
        val layout = CustomTxNotificationViewHolder(context, tx)
        val intent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            flags = FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HomeDeeplinkScreens.Key, HomeDeeplinkScreens.TxDetails.name)
            putExtra(HomeDeeplinkScreens.KeyTxDetailsArgs, TxId(tx.id))
        }
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        // prepare transaction notification
        val notification: Notification = NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_ID).run {
            setContentTitle(notificationTitle)
            setContentText(notificationBody)
            setSmallIcon(R.drawable.tx_notification_icon)
            setDefaults(DEFAULT_ALL)
            setContentIntent(pendingIntent)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setCustomContentView(layout)
            setAutoCancel(true)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setCategory(NotificationCompat.CATEGORY_EVENT)
            priority = NotificationCompat.PRIORITY_MAX
            build()
        }

        // send group notification
        notificationManager.notify(APP_NOTIFICATION_GROUP_ID, txGroupNotification)
        // send actual notification
        notificationManager.notify(tx.id.toInt(), notification)
    }

    fun postTxCanceledNotification(tx: CancelledTx) {
        logger.i("postTxCanceledNotification: ${tx.id}")
        val layout = TxCanceledViewHolder(context, tx)
        val intent = Intent(context, NotificationBroadcastReceiver::class.java).apply {
            flags = FLAG_ACTIVITY_CLEAR_TOP
            putExtra(HomeDeeplinkScreens.Key, HomeDeeplinkScreens.TxDetails.name)
            putExtra(HomeDeeplinkScreens.KeyTxDetailsArgs, TxId(tx.id))
        }

        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_ID).run {
            setSmallIcon(R.drawable.tx_notification_icon)
            setDefaults(DEFAULT_ALL)
            setContentIntent(pendingIntent)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setCustomContentView(layout)
            setAutoCancel(true)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setCategory(NotificationCompat.CATEGORY_EVENT)
            priority = NotificationCompat.PRIORITY_MAX
            build()
        }
        // send group notification
        notificationManager.notify(APP_NOTIFICATION_GROUP_ID, txGroupNotification)
        // send actual notification
        notificationManager.notify(tx.id.toInt(), notification)
    }

    /**
     * Posts standard Android heads-up notification.
     */
    fun postNotification(title: String, body: String) {
        // prepare notification
        val notification = NotificationCompat.Builder(context, APP_NOTIFICATION_CHANNEL_ID).run {
            setContentTitle(title)
            setContentText(body)
            setSmallIcon(R.drawable.notification_icon)
            setDefaults(DEFAULT_ALL)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setCategory(NotificationCompat.CATEGORY_EVENT)
            priority = NotificationCompat.PRIORITY_MAX
            build()
        }

        // send notification
        notificationManager.notify(APP_NOTIFICATION_GROUP_ID, txGroupNotification)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
