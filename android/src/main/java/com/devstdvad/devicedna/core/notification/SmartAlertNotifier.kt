package com.devstdvad.devicedna.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.devstdvad.devicedna.MainActivity
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.data.alerts.SmartAlertType

/** Posts (and clears) Smart Alerts system notifications. One stable id per alert type. */
class SmartAlertNotifier(private val context: Context) {

    fun notify(type: SmartAlertType) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, type.route)
        }
        val pending = PendingIntent.getActivity(
            context,
            type.ordinal,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val body = context.getString(type.bodyRes)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(type.titleRes))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId(type), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — ignore.
        }
    }

    /** Removes a previously-posted alert once its condition clears. */
    fun cancel(type: SmartAlertType) {
        NotificationManagerCompat.from(context).cancel(notificationId(type))
    }

    private fun notificationId(type: SmartAlertType) = NOTIFICATION_ID_BASE + type.ordinal

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_smart_alerts),
            NotificationManager.IMPORTANCE_HIGH,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "smart_alerts"
        const val NOTIFICATION_ID_BASE = 4300
    }
}
