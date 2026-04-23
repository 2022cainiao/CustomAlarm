package com.customalarm.app.util

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import com.customalarm.app.data.model.AlarmEntity
import java.util.Calendar

data class SystemAlarmDraft(
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: List<Int>,
    val holidayAwareWorkdays: Boolean,
    val vibrate: Boolean
)

enum class SystemAlarmLaunchResult {
    Launched,
    LaunchedWithHolidayFallback,
    InvalidTime,
    NotSupported
}

fun AlarmEntity.toSystemAlarmDraft(): SystemAlarmDraft {
    return SystemAlarmDraft(
        hour = hour,
        minute = minute,
        label = label,
        repeatDays = repeatDays,
        holidayAwareWorkdays = holidayAwareWorkdays,
        vibrate = vibrate
    )
}

fun launchSystemAlarm(context: Context, draft: SystemAlarmDraft): SystemAlarmLaunchResult {
    if (draft.hour !in 0..23 || draft.minute !in 0..59) {
        return SystemAlarmLaunchResult.InvalidTime
    }

    val mappedDays = draft.repeatDays.mapNotNull(::toCalendarDayOfWeek)
    val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(AlarmClock.EXTRA_HOUR, draft.hour)
        putExtra(AlarmClock.EXTRA_MINUTES, draft.minute)
        putExtra(AlarmClock.EXTRA_MESSAGE, draft.label)
        putExtra(AlarmClock.EXTRA_VIBRATE, draft.vibrate)
        if (mappedDays.isNotEmpty()) {
            putIntegerArrayListExtra(AlarmClock.EXTRA_DAYS, ArrayList(mappedDays))
        }
    }

    val activity = intent.resolveActivity(context.packageManager) ?: return SystemAlarmLaunchResult.NotSupported
    intent.setPackage(activity.packageName)
    context.startActivity(intent)
    return if (draft.holidayAwareWorkdays) {
        SystemAlarmLaunchResult.LaunchedWithHolidayFallback
    } else {
        SystemAlarmLaunchResult.Launched
    }
}

private fun toCalendarDayOfWeek(day: Int): Int? {
    return when (day) {
        1 -> Calendar.MONDAY
        2 -> Calendar.TUESDAY
        3 -> Calendar.WEDNESDAY
        4 -> Calendar.THURSDAY
        5 -> Calendar.FRIDAY
        6 -> Calendar.SATURDAY
        7 -> Calendar.SUNDAY
        else -> null
    }
}
