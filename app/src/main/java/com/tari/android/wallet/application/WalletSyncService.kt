package com.tari.android.wallet.application

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.orhanobut.logger.Logger
import com.orhanobut.logger.Printer
import com.tari.android.wallet.R

class WalletSyncService : Service() {

    private val logger: Printer
        get() = Logger.t(this::class.simpleName)

    private val notificationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == ACTION_UPDATE_NOTIFICATION) {
                val newText = intent.getStringExtra(EXTRA_NOTIFICATION_TEXT) ?: return
                updateNotificationText(newText)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        registerReceiver(
            /* receiver = */ notificationReceiver,
            /* filter = */ IntentFilter(ACTION_UPDATE_NOTIFICATION),
            /* flags = */ Context.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.i("WalletSyncService started with ID: $startId")

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wallet_sync_service_notification_title))
            .setContentText(getString(R.string.wallet_sync_service_notification_message))
            .setSmallIcon(R.drawable.vector_notification_icon)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        unregisterReceiver(notificationReceiver)
        instance = null
        super.onDestroy()

        logger.i("WalletSyncService destroyed with ID: ${this@WalletSyncService}")
    }

    private fun createNotificationChannel() {
        getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(
                NotificationChannel(
                    /* id = */ CHANNEL_ID,
                    /* name = */ "Wallet Sync Service",
                    /* importance = */ NotificationManager.IMPORTANCE_LOW,
                )
            )
    }

    private fun updateNotificationText(newText: String) {
        val updatedNotification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.wallet_sync_service_notification_title))
            .setContentText(newText)
            .setSmallIcon(R.drawable.vector_notification_icon)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, updatedNotification)
    }

    companion object {

        private const val CHANNEL_ID = "wallet_sync_service_channel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_UPDATE_NOTIFICATION = "com.tari.android.wallet.UPDATE_NOTIFICATION"
        const val EXTRA_NOTIFICATION_TEXT = "notification_text"

        @Volatile
        var instance: WalletSyncService? = null

        fun startService(context: Context) {
            if (instance == null) {
                context.startForegroundService(Intent(context, WalletSyncService::class.java))
            }
        }

        fun stopIfRunning(context: Context) {
            instance?.let {
                context.stopService(Intent(context, WalletSyncService::class.java))
            }
        }

        fun updateNotification(context: Context, newText: String) {
            context.sendBroadcast(
                Intent(ACTION_UPDATE_NOTIFICATION).apply {
                    setPackage(context.packageName)
                    putExtra(EXTRA_NOTIFICATION_TEXT, newText)
                }
            )
        }
    }
}