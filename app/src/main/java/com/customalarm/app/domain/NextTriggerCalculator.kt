package com.customalarm.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class NextTriggerCalculator(
    private val holidayCalendar: HolidayCalendar = HolidayCalendar(),
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun calculateNextTrigger(
        hour: Int,
        minute: Int,
        repeatDays: List<Int>,
        holidayAwareWorkdays: Boolean = false,
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        val from = Instant.ofEpochMilli(fromMillis).atZone(zoneId)
        return if (repeatDays.isEmpty()) {
            calculateOneTime(hour = hour, minute = minute, from = from)
        } else if (holidayAwareWorkdays) {
            calculateHolidayAwareWorkdays(hour = hour, minute = minute, from = from)
        } else {
            calculateRepeating(hour = hour, minute = minute, repeatDays = repeatDays, from = from)
        }
    }

    private fun calculateOneTime(hour: Int, minute: Int, from: ZonedDateTime): Long {
        var candidate = from.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusDays(1)
        }
        return candidate.toInstant().toEpochMilli()
    }

    private fun calculateRepeating(
        hour: Int,
        minute: Int,
        repeatDays: List<Int>,
        from: ZonedDateTime
    ): Long {
        val allowedDays = repeatDays.toSet()
        for (offset in 0..7) {
            val candidate = from.plusDays(offset.toLong())
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0)
            val dayValue = candidate.dayOfWeek.toLegacyValue()
            if (dayValue in allowedDays && candidate.isAfter(from)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        return calculateOneTime(hour = hour, minute = minute, from = from)
    }

    private fun calculateHolidayAwareWorkdays(
        hour: Int,
        minute: Int,
        from: ZonedDateTime
    ): Long {
        for (offset in 0..370) {
            val candidate = from.plusDays(offset.toLong())
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .withNano(0)
            if (holidayCalendar.isOfficialWorkday(candidate.toLocalDate()) && candidate.isAfter(from)) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        return calculateOneTime(hour = hour, minute = minute, from = from)
    }
}

private fun DayOfWeek.toLegacyValue(): Int = when (this) {
    DayOfWeek.MONDAY -> 1
    DayOfWeek.TUESDAY -> 2
    DayOfWeek.WEDNESDAY -> 3
    DayOfWeek.THURSDAY -> 4
    DayOfWeek.FRIDAY -> 5
    DayOfWeek.SATURDAY -> 6
    DayOfWeek.SUNDAY -> 7
}
