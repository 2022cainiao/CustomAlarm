package com.customalarm.app.domain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

data class HolidayCalendar(
    private val holidayDates: Set<LocalDate> = emptySet(),
    private val makeupWorkdays: Set<LocalDate> = emptySet()
) {
    fun isHoliday(date: LocalDate): Boolean = holidayDates.contains(date)

    fun isMakeupWorkday(date: LocalDate): Boolean = makeupWorkdays.contains(date)

    fun isOfficialWorkday(date: LocalDate): Boolean {
        if (isMakeupWorkday(date)) return true
        if (isHoliday(date)) return false
        return date.dayOfWeek in STANDARD_WORKDAYS
    }

    companion object {
        fun empty(): HolidayCalendar = HolidayCalendar()

        fun fromJson(json: String): HolidayCalendar {
            val root = JSONObject(json)
            return HolidayCalendar(
                holidayDates = root.optJSONArray("holidays").toLocalDateSet(),
                makeupWorkdays = root.optJSONArray("makeupWorkdays").toLocalDateSet()
            )
        }

        private val STANDARD_WORKDAYS = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )
    }
}

class HolidayCalendarStore(
    private val context: Context
) {
    @Volatile
    private var currentCalendar: HolidayCalendar = loadInitialCalendar()

    fun currentCalendar(): HolidayCalendar = currentCalendar

    fun updateFromJson(json: String): Boolean {
        val parsed = runCatching { HolidayCalendar.fromJson(json) }.getOrNull() ?: return false
        runCatching { cacheFile().writeText(json) }.getOrNull() ?: return false
        currentCalendar = parsed
        return true
    }

    fun resetToBundledCalendar(): HolidayCalendar {
        cacheFile().delete()
        return loadBundledCalendar().also { currentCalendar = it }
    }

    private fun loadInitialCalendar(): HolidayCalendar {
        return loadCachedCalendar() ?: loadBundledCalendar()
    }

    private fun loadCachedCalendar(): HolidayCalendar? {
        val file = cacheFile()
        if (!file.exists()) return null
        return runCatching { HolidayCalendar.fromJson(file.readText()) }.getOrNull()
    }

    private fun loadBundledCalendar(): HolidayCalendar {
        return runCatching {
            context.assets.open(BUNDLED_ASSET_NAME).bufferedReader().use { reader ->
                HolidayCalendar.fromJson(reader.readText())
            }
        }.getOrElse { HolidayCalendar.empty() }
    }

    private fun cacheFile(): File = File(context.filesDir, CACHE_FILE_NAME)

    private companion object {
        const val BUNDLED_ASSET_NAME = "holiday_calendar.json"
        const val CACHE_FILE_NAME = "holiday_calendar_cache.json"
    }
}

private fun JSONArray?.toLocalDateSet(): Set<LocalDate> {
    if (this == null) return emptySet()
    val result = mutableSetOf<LocalDate>()
    for (index in 0 until length()) {
        val raw = optString(index).trim()
        if (raw.isNotEmpty()) {
            result += LocalDate.parse(raw)
        }
    }
    return result
}
