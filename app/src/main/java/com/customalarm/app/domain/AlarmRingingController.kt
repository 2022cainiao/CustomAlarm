package com.customalarm.app.domain

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.customalarm.app.R
import com.customalarm.app.service.AlarmPlaybackService
import com.customalarm.app.ui.ringing.RingingActivity

class AlarmRingingController(private val context: Context) {
    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotification(
        alarmId: Long,
        label: String,
        snoozeEnabled: Boolean,
        snoozeMinutes: Int
    ): Notification {
        ensureChannel()
        val title = if (label.isBlank()) context.getString(R.string.app_name) else label
        val contentIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            RingingActivity.createIntent(
                context = context,
                alarmId = alarmId,
                label = label,
                snoozeEnabled = snoozeEnabled,
                snoozeMinutes = snoozeMinutes
            ),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = PendingIntent.getService(
            context,
            (alarmId * 2).toInt(),
            AlarmPlaybackService.createDismissIntent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = PendingIntent.getService(
            context,
            (alarmId * 2 + 1).toInt(),
            AlarmPlaybackService.createSnoozeIntent(context, alarmId, snoozeMinutes),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("Alarm ringing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(0, "Dismiss", dismissIntent)

        if (snoozeEnabled) {
            builder.addAction(0, "Snooze ${snoozeMinutes} min", snoozeIntent)
        }

        return builder.build()
    }

    fun defaultAlarmUri() = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Alarm alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications for ringing alarms"
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "alarm_ringing"
        const val NOTIFICATION_ID_BASE = 4_200
        const val ACTION_FINISH_RINGING = "com.customalarm.app.action.FINISH_RINGING"
    }
}
