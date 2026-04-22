package com.customalarm.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.US)

fun formatAlarmTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun formatRepeatDays(days: List<Int>): String {
    if (days.isEmpty()) return "One time"

    val sorted = days.sorted()
    val weekDayMap = mapOf(
        1 to "Mon",
        2 to "Tue",
        3 to "Wed",
        4 to "Thu",
        5 to "Fri",
        6 to "Sat",
        7 to "Sun"
    )

    if (sorted == listOf(1, 2, 3, 4, 5)) return "Workdays"
    if (sorted == listOf(6, 7)) return "Weekend"
    if (sorted == listOf(1, 2, 3, 4, 5, 6, 7)) return "Every day"

    return sorted.joinToString(" ") { weekDayMap[it].orEmpty() }
}

fun formatNextTrigger(triggerAt: Long?): String {
    if (triggerAt == null) return "Disabled"
    return Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .format(dateTimeFormatter)
}

fun formatRingingClock(timestamp: Long = System.currentTimeMillis()): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}
