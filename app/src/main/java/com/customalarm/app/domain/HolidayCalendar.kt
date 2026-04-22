package com.customalarm.app.domain

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class HolidayCalendar(
    private val holidayDates: Set<LocalDate> = emptySet(),
    private val makeupWorkdays: Set<LocalDate> = emptySet(),
    val version: String? = null,
    val region: String = "CN",
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val syncedAt: Instant? = null,
    val validThrough: LocalDate? = null
) {
    fun isHoliday(date: LocalDate): Boolean = holidayDates.contains(date)

    fun isMakeupWorkday(date: LocalDate): Boolean = makeupWorkdays.contains(date)

    fun isOfficialWorkday(date: LocalDate): Boolean {
        if (isMakeupWorkday(date)) return true
        if (isHoliday(date)) return false
        return date.dayOfWeek in STANDARD_WORKDAYS
    }

    fun latestCoveredDate(): LocalDate? = validThrough ?: (holidayDates + makeupWorkdays).maxOrNull()

    fun isExpired(today: LocalDate = LocalDate.now()): Boolean {
        val coverageEnd = latestCoveredDate() ?: return true
        return coverageEnd.isBefore(today)
    }

    fun toJson(): String {
        val root = JSONObject()
        version?.let { root.put("version", it) }
        root.put("region", region)
        sourceName?.let { root.put("sourceName", it) }
        sourceUrl?.let { root.put("sourceUrl", it) }
        syncedAt?.let { root.put("syncedAt", it.toString()) }
        latestCoveredDate()?.let { root.put("validThrough", it.toString()) }
        root.put("holidays", JSONArray(holidayDates.sorted().map(LocalDate::toString)))
        root.put("makeupWorkdays", JSONArray(makeupWorkdays.sorted().map(LocalDate::toString)))
        return root.toString(2)
    }

    companion object {
        fun empty(): HolidayCalendar = HolidayCalendar()

        fun fromJson(json: String): HolidayCalendar {
            val root = JSONObject(json)
            return when {
                root.has("holidays") || root.has("makeupWorkdays") -> fromNormalizedJson(root)
                root.has("Years") -> fromPublicHolidayApi(root)
                else -> error("Unsupported holiday calendar format")
            }
        }

        private fun fromNormalizedJson(root: JSONObject): HolidayCalendar {
            val holidays = root.optJSONArray("holidays").toLocalDateSet()
            val makeupWorkdays = root.optJSONArray("makeupWorkdays").toLocalDateSet()
            return HolidayCalendar(
                holidayDates = holidays,
                makeupWorkdays = makeupWorkdays,
                version = root.optString("version").ifBlank { null },
                region = root.optString("region").ifBlank { "CN" },
                sourceName = root.optString("sourceName").ifBlank { null },
                sourceUrl = root.optString("sourceUrl").ifBlank { null },
                syncedAt = root.optString("syncedAt").toInstantOrNull(),
                validThrough = root.optString("validThrough").toLocalDateOrNull()
                    ?: (holidays + makeupWorkdays).maxOrNull()
            )
        }

        private fun fromPublicHolidayApi(root: JSONObject): HolidayCalendar {
            val holidays = mutableSetOf<LocalDate>()
            val makeupWorkdays = mutableSetOf<LocalDate>()
            val years = root.optJSONObject("Years") ?: error("Missing Years block")
            val yearKeys = years.keys()
            while (yearKeys.hasNext()) {
                val yearKey = yearKeys.next()
                val entries = years.optJSONArray(yearKey) ?: continue
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    val startDate = entry.optString("StartDate").toLocalDateOrNull() ?: continue
                    val endDate = entry.optString("EndDate").toLocalDateOrNull() ?: startDate
                    holidays += expandDateRange(startDate, endDate)
                    makeupWorkdays += entry.optJSONArray("CompDays").toLocalDateSet()
                }
            }

            return HolidayCalendar(
                holidayDates = holidays,
                makeupWorkdays = makeupWorkdays,
                version = root.optString("Generated").ifBlank {
                    root.optString("Version").ifBlank { null }
                },
                region = "CN",
                sourceName = root.optString("Name").ifBlank { "Public holiday feed" },
                sourceUrl = root.optString("URL").ifBlank { null },
                syncedAt = root.optString("Generated").toGeneratedInstantOrNull(),
                validThrough = (holidays + makeupWorkdays).maxOrNull()
            )
        }

        private val STANDARD_WORKDAYS = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        )

        private fun expandDateRange(start: LocalDate, end: LocalDate): Set<LocalDate> {
            if (end.isBefore(start)) return setOf(start)
            val days = mutableSetOf<LocalDate>()
            var cursor = start
            while (!cursor.isAfter(end)) {
                days += cursor
                cursor = cursor.plusDays(1)
            }
            return days
        }
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
        return update(parsed)
    }

    fun update(calendar: HolidayCalendar): Boolean {
        runCatching { cacheFile().writeText(calendar.toJson()) }.getOrNull() ?: return false
        currentCalendar = calendar
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
                HolidayCalendar.fromJson(reader.readText()).let { calendar ->
                    if (calendar.sourceName != null) {
                        calendar
                    } else {
                        calendar.copy(sourceName = "Bundled calendar")
                    }
                }
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

private fun String.toLocalDateOrNull(): LocalDate? = runCatching {
    if (isBlank()) null else LocalDate.parse(this)
}.getOrNull()

private fun String.toInstantOrNull(): Instant? = runCatching {
    if (isBlank()) null else Instant.parse(this)
}.getOrNull()

private fun String.toGeneratedInstantOrNull(): Instant? = runCatching {
    if (isBlank()) {
        null
    } else {
        OffsetDateTime.parse(this, GENERATED_TIMESTAMP_FORMATTER).toInstant()
    }
}.getOrNull()

private val GENERATED_TIMESTAMP_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX").withZone(ZoneOffset.UTC)
