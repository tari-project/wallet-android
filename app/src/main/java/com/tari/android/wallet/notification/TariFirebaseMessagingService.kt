package com.tari.android.wallet.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.orhanobut.logger.Logger
import com.tari.android.wallet.R
import com.tari.android.wallet.notification.NotificationHelper.Companion.SERVICE_NOTIFICATION_CHANNEL_ID
import com.tari.android.wallet.ui.screen.home.HomeActivity

class TariFirebaseMessagingService : FirebaseMessagingService() {

    private val logger
        get() = Logger.t(TariFirebaseMessagingService::class.simpleName)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        logger.d("From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            logger.d("Message data payload: ${remoteMessage.data}")

            handleMessage(remoteMessage)
        }
    }

    override fun onNewToken(token: String) {
        logger.d("Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        sendRegistrationToServer(token)
    }

    private fun handleMessage(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            logger.d("Message Notification Body: ${it.body}")
            it.body?.let { body -> sendNotification(body) }
        }
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * // TODO Remove after testing!!
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String) {
        val requestCode = 0
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            /* context = */ this,
            /* requestCode = */ requestCode,
            /* intent = */ intent,
            /* flags = */ PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = SERVICE_NOTIFICATION_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.vector_icon_send_tari)
            .setContentTitle("FCM message test!!")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        val channel = NotificationChannel(
            /* id = */ channelId,
            /* name = */ "Channel human readable title",
            /* importance = */ NotificationManager.IMPORTANCE_DEFAULT,
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}