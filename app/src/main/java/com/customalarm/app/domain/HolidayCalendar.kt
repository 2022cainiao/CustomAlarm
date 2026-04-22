package com.customalarm.app.domain

import java.time.DayOfWeek
import java.time.LocalDate

class HolidayCalendar {
    fun isHoliday(date: LocalDate): Boolean = holidayDates.contains(date)

    fun isMakeupWorkday(date: LocalDate): Boolean = makeupWorkdays.contains(date)

    fun isOfficialWorkday(date: LocalDate): Boolean {
        if (isMakeupWorkday(date)) return true
        if (isHoliday(date)) return false
        return date.dayOfWeek in setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    }
}

private val holidayDates: Set<LocalDate> = buildSet {
    // Official PRC public holiday dates currently embedded for 2025 and 2026.
    addRange(2025, 1, 1, 1, 1)
    addRange(2025, 1, 28, 2, 4)
    addRange(2025, 4, 4, 4, 6)
    addRange(2025, 5, 1, 5, 5)
    addRange(2025, 5, 31, 6, 2)
    addRange(2025, 10, 1, 10, 8)

    addRange(2026, 1, 1, 1, 3)
    addRange(2026, 2, 15, 2, 23)
    addRange(2026, 4, 4, 4, 6)
    addRange(2026, 5, 1, 5, 5)
    addRange(2026, 6, 19, 6, 21)
    addRange(2026, 9, 25, 9, 27)
    addRange(2026, 10, 1, 10, 7)
}

private val makeupWorkdays: Set<LocalDate> = setOf(
    LocalDate.of(2025, 1, 26),
    LocalDate.of(2025, 2, 8),
    LocalDate.of(2025, 4, 27),
    LocalDate.of(2025, 9, 28),
    LocalDate.of(2025, 10, 11),
    LocalDate.of(2026, 1, 4),
    LocalDate.of(2026, 2, 14),
    LocalDate.of(2026, 2, 28),
    LocalDate.of(2026, 5, 9),
    LocalDate.of(2026, 9, 20),
    LocalDate.of(2026, 10, 10)
)

private fun MutableSet<LocalDate>.addRange(
    startYear: Int,
    startMonth: Int,
    startDay: Int,
    endMonth: Int,
    endDay: Int
) {
    var cursor = LocalDate.of(startYear, startMonth, startDay)
    val end = LocalDate.of(startYear, endMonth, endDay)
    while (!cursor.isAfter(end)) {
        add(cursor)
        cursor = cursor.plusDays(1)
    }
}
