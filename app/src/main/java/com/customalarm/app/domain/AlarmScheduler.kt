package com.customalarm.app.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.repository.AlarmRepository
import com.customalarm.app.receiver.AlarmReceiver

class AlarmScheduler(
    private val context: Context,
    private val alarmRepository: AlarmRepository
) {
    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    suspend fun refreshAll() {
        alarmRepository.getScheduleCandidates().forEach { candidate ->
            try {
                cancelAlarm(candidate.alarm.id)
                val nextTriggerAt = candidate.alarm.nextTriggerAt
                if (candidate.isEffectivelyEnabled && nextTriggerAt != null) {
                    scheduleAlarm(candidate.alarm, nextTriggerAt)
                }
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to refresh alarm ${candidate.alarm.id}", exception)
            }
        }
    }

    fun cancelAlarm(alarmId: Long) {
        alarmManager.cancel(pendingIntent(alarmId))
    }

    private fun scheduleAlarm(alarm: AlarmEntity, triggerAtMillis: Long) {
        val pendingIntent = pendingIntent(alarm.id)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    private fun pendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"
    }
}
