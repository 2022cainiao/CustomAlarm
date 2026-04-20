package com.customalarm.app.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
private val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.CHINA)

fun formatAlarmTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

fun formatRepeatDays(days: List<Int>): String {
    if (days.isEmpty()) return "仅一次"
    val weekDayMap = mapOf(
        1 to "周一",
        2 to "周二",
        3 to "周三",
        4 to "周四",
        5 to "周五",
        6 to "周六",
        7 to "周日"
    )
    if (days == listOf(1, 2, 3, 4, 5)) return "工作日"
    if (days == listOf(6, 7)) return "周末"
    if (days == listOf(1, 2, 3, 4, 5, 6, 7)) return "每天"
    return days.sorted().joinToString(" ") { weekDayMap[it].orEmpty() }
}

fun formatNextTrigger(triggerAt: Long?): String {
    if (triggerAt == null) return "未启用"
    return Instant.ofEpochMilli(triggerAt)
        .atZone(ZoneId.systemDefault())
        .format(dateTimeFormatter)
}

fun formatRingingClock(timestamp: Long = System.currentTimeMillis()): String {
    return Instant.ofEpochMilli(timestamp)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}

