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

/**
 * One-off system notification announcing the new home-screen widgets feature.
 * Tapping it deep-links to the Widgets screen (reuses [MainActivity.EXTRA_ROUTE]).
 */
object WidgetPromoNotifier {

    private const val CHANNEL_ID = "feature_updates"
    private const val NOTIFICATION_ID = 4201

    fun show(context: Context) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "widgets")
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.notif_widgets_title))
            .setContentText(context.getString(R.string.notif_widgets_body))
            .setStyle(NotificationCompat.BigTextStyle().bigText(context.getString(R.string.notif_widgets_body)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — ignore.
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_features),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
