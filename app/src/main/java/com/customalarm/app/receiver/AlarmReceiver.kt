package com.customalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.customalarm.app.appContainer
import com.customalarm.app.service.AlarmPlaybackService
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return

        val pendingResult = goAsync()
        val container = context.appContainer
        container.applicationScope.launch {
            val alarm = container.alarmRepository.getAlarm(alarmId)
            if (alarm != null) {
                container.alarmCoordinator.onAlarmTriggered(alarmId)
                ContextCompat.startForegroundService(
                    context,
                    AlarmPlaybackService.createStartIntent(
                        context = context,
                        alarmId = alarm.id,
                        label = alarm.label,
                        ringtoneUri = alarm.ringtoneUri,
                        vibrate = alarm.vibrate,
                        snoozeEnabled = alarm.snoozeEnabled,
                        snoozeMinutes = alarm.snoozeMinutes
                    )
                )
            }
            pendingResult.finish()
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
    }
}

