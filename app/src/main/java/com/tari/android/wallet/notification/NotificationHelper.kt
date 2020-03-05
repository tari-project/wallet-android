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
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.tari.android.wallet.R
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.activity.tx.TxDetailActivity
import com.tari.android.wallet.ui.notification.CustomTxNotificationViewHolder
import com.tari.android.wallet.util.WalletUtil

/**
 * Contains helper functions for building and posting notifications.
 *
 * @author The Tari Development Team
 */
internal class NotificationHelper(private val context: Context) {

    companion object {
        // notification channel id
        private const val SERVICE_NOTIFICATION_CHANNEL_ID =
            "com.tari.android.wallet.service.WALLET_SERVICE_NOTIFICATION"
        private const val APP_NOTIFICATION_CHANNEL_ID =
            "com.tari.android.wallet.WALLET_NOTIFICATION"
        private const val APP_NOTIFICATION_GROUP_ID = 1000
        private const val APP_NOTIFICATION_GROUP_NAME = "com.tari.android.wallet.notification.TX"
    }

    private var notificationManager = NotificationManagerCompat.from(context)

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
        }
    }

    fun buildForegroundServiceNotification(): Notification {
        // prepare foreground service notification
        return NotificationCompat.Builder(
            context,
            SERVICE_NOTIFICATION_CHANNEL_ID
        ).run {
            setContentTitle(context.getString(R.string.wallet_service_title))
            setContentText(context.getString(R.string.wallet_service_description))
            setSound(null)
            setSmallIcon(R.drawable.notification_icon)
            build()
        }
    }

    private val getTxGroupNotification: Notification =
        NotificationCompat.Builder(
            context,
            APP_NOTIFICATION_CHANNEL_ID
        ).run {
            setGroupSummary(true)
            setSmallIcon(R.drawable.home_tx_icon)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setAutoCancel(true)
            setGroupSummary(true)
            build()
        }

    /**
     * Posts standard Android heads-up transaction notification.
     */
    fun postTxNotification(tx: Tx) {
        // title
        val notificationTitle = context.getString(R.string.notification_tx_received_title)
        // description
        val formattedAmount = if (tx.amount.tariValue.toDouble() % 1 == 0.toDouble()) {
            tx.amount.tariValue.toBigInteger().toString()
        } else {
            WalletUtil.amountFormatter.format(tx.amount.tariValue)
        }
        val notificationBody = String.format(
            context.getString(R.string.notification_tx_received_description_format),
            formattedAmount
        )

        val intent = TxDetailActivity.createIntent(context, tx)
        // val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        // Create the TaskStackBuilder
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        // prepare notification
        val notification: Notification = NotificationCompat.Builder(
            context,
            APP_NOTIFICATION_CHANNEL_ID
        ).run {
            setContentTitle(notificationTitle)
            setContentText(notificationBody)
            setSmallIcon(R.drawable.tx_notification_icon)
            setDefaults(DEFAULT_ALL)
            setContentIntent(pendingIntent)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setCategory(NotificationCompat.CATEGORY_EVENT)
            priority = NotificationCompat.PRIORITY_MAX
            build()
        }

        // send notification
        notificationManager.notify(
            APP_NOTIFICATION_GROUP_ID,
            getTxGroupNotification
        )
        notificationManager.notify(
            tx.id.toInt(),
            notification
        )
    }

    /**
     * Posts custom-layout heads-up transaction notification.
     */
    fun postCustomLayoutTxNotification(tx: Tx) {
        val notificationTitle = context.getString(R.string.notification_tx_received_title)
        // format spannable string
        val formattedAmount = if (tx.amount.tariValue.toDouble() % 1 == 0.toDouble()) {
            tx.amount.tariValue.toBigInteger().toString()
        } else {
            WalletUtil.amountFormatter.format(tx.amount.tariValue)
        }
        val notificationBody = String.format(
            context.getString(R.string.notification_tx_received_description_format),
            formattedAmount
        )

        val layout = CustomTxNotificationViewHolder(
            context,
            tx
        )
        val intent = TxDetailActivity.createIntent(context, tx)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
        /*
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(intent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        }
         */

        // prepare transaction notification
        val notification: Notification = NotificationCompat.Builder(
            context,
            APP_NOTIFICATION_CHANNEL_ID
        ).run {
            setContentTitle(notificationTitle)
            setContentText(notificationBody)
            setSmallIcon(R.drawable.tx_notification_icon)
            setDefaults(DEFAULT_ALL)
            setContentIntent(pendingIntent)
            setStyle(NotificationCompat.DecoratedCustomViewStyle())
            setCustomContentView(layout)
            setAutoCancel(true)
            //setCustomBigContentView(notificationLayout)
            //setCustomHeadsUpContentView(notificationLayout)
            setGroup(APP_NOTIFICATION_GROUP_NAME)
            setCategory(NotificationCompat.CATEGORY_EVENT)
            priority = NotificationCompat.PRIORITY_MAX
            build()
        }

        // send group notification
        notificationManager.notify(
            APP_NOTIFICATION_GROUP_ID,
            getTxGroupNotification
        )
        // send actual notification
        notificationManager.notify(
            tx.id.toInt(),
            notification
        )
    }

}