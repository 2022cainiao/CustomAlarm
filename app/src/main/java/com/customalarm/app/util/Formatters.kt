package com.customalarm.app.util

import android.content.Context
import com.customalarm.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun formatAlarmTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun formatRepeatDays(
    context: Context,
    days: List<Int>,
    holidayAwareWorkdays: Boolean = false
): String {
    if (days.isEmpty()) return context.getString(R.string.repeat_one_time)
    if (holidayAwareWorkdays) return context.getString(R.string.repeat_official_workdays)

    val sorted = days.sorted()
    val weekDayMap = mapOf(
        1 to context.getString(R.string.day_mon_short),
        2 to context.getString(R.string.day_tue_short),
        3 to context.getString(R.string.day_wed_short),
        4 to context.getString(R.string.day_thu_short),
        5 to context.getString(R.string.day_fri_short),
        6 to context.getString(R.string.day_sat_short),
        7 to context.getString(R.string.day_sun_short)
    )

    if (sorted == listOf(1, 2, 3, 4, 5)) return context.getString(R.string.repeat_workdays)
    if (sorted == listOf(6, 7)) return context.getString(R.string.repeat_weekend)
    if (sorted == listOf(1, 2, 3, 4, 5, 6, 7)) return context.getString(R.string.repeat_every_day)

    return sorted.joinToString(" ") { weekDayMap[it].orEmpty() }
}

fun formatNextTrigger(context: Context, triggerAt: Long?): String {
    if (triggerAt == null) return context.getString(R.string.label_disabled)
    return Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .format(dateTimeFormatter)
}

fun formatRingingClock(timestamp: Long = System.currentTimeMillis()): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}
