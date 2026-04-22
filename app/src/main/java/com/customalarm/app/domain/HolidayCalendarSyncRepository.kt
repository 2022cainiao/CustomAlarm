package com.customalarm.app.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate

data class HolidayCalendarSyncStatus(
    val coverageEnd: LocalDate? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val syncedAt: Instant? = null,
    val isExpired: Boolean = false,
    val isSyncing: Boolean = false,
    val lastErrorMessage: String? = null,
    val shouldWarnSourceUnavailable: Boolean = false
)

data class HolidayCalendarSyncResult(
    val succeeded: Boolean,
    val calendarChanged: Boolean,
    val errorMessage: String? = null
)

data class HolidayCalendarRemoteSource(
    val name: String,
    val url: String
)

class HolidayCalendarSyncRepository(
    private val holidayCalendarStore: HolidayCalendarStore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
    private val nowProvider: () -> Instant = { Instant.now() },
    private val remoteSources: List<HolidayCalendarRemoteSource> = emptyList()
) {
    private val _status = MutableStateFlow(buildStatus())
    val status: StateFlow<HolidayCalendarSyncStatus> = _status.asStateFlow()
    private val syncMutex = Mutex()

    @Volatile
    private var autoSyncAttempted = false

    private var lastSyncErrorMessage: String? = null
    private var lastSyncFailed = false

    fun refreshStatus() {
        _status.value = buildStatus(isSyncing = _status.value.isSyncing)
    }

    suspend fun syncIfNeeded(): HolidayCalendarSyncResult {
        if (autoSyncAttempted) {
            refreshStatus()
            return HolidayCalendarSyncResult(
                succeeded = true,
                calendarChanged = false
            )
        }
        autoSyncAttempted = true
        return syncNow()
    }

    suspend fun syncNow(): HolidayCalendarSyncResult {
        return syncMutex.withLock {
            _status.value = buildStatus(isSyncing = true)
            val result = runCatching {
                when (val fetchResult = fetchRemoteCalendar()) {
                    is RemoteFetchResult.Success -> {
                        val normalizedCalendar = fetchResult.calendar.copy(
                            sourceName = fetchResult.calendar.sourceName ?: fetchResult.source.name,
                            sourceUrl = fetchResult.source.url,
                            syncedAt = nowProvider()
                        )
                        val updated = withContext(ioDispatcher) {
                            holidayCalendarStore.update(normalizedCalendar)
                        }
                        if (updated) {
                            lastSyncErrorMessage = null
                            lastSyncFailed = false
                            HolidayCalendarSyncResult(
                                succeeded = true,
                                calendarChanged = true
                            )
                        } else {
                            val message = "Unable to cache synced holiday data."
                            lastSyncErrorMessage = message
                            lastSyncFailed = true
                            HolidayCalendarSyncResult(
                                succeeded = false,
                                calendarChanged = false,
                                errorMessage = message
                            )
                        }
                    }

                    is RemoteFetchResult.Failure -> {
                        lastSyncErrorMessage = fetchResult.message
                        lastSyncFailed = true
                        HolidayCalendarSyncResult(
                            succeeded = false,
                            calendarChanged = false,
                            errorMessage = fetchResult.message
                        )
                    }
                }
            }.getOrElse { throwable ->
                val message = throwable.message.orEmpty().ifBlank { "Holiday calendar sync failed." }
                lastSyncErrorMessage = message
                lastSyncFailed = true
                HolidayCalendarSyncResult(
                    succeeded = false,
                    calendarChanged = false,
                    errorMessage = message
                )
            }
            _status.value = buildStatus()
            result
        }
    }

    private suspend fun fetchRemoteCalendar(): RemoteFetchResult = withContext(ioDispatcher) {
        var lastError = "Holiday server endpoint is not configured."
        for (source in remoteSources) {
            val responseResult = runCatching { download(source.url) }
            if (responseResult.isFailure) {
                val throwable = responseResult.exceptionOrNull()
                lastError = "${source.name}: ${throwable?.message.orEmpty().ifBlank { "request failed" }}"
                continue
            }
            val parsedResult = runCatching { HolidayCalendar.fromJson(responseResult.getOrThrow()) }
            if (parsedResult.isFailure) {
                val throwable = parsedResult.exceptionOrNull()
                lastError = "${source.name}: ${throwable?.message.orEmpty().ifBlank { "invalid JSON format" }}"
                continue
            }
            return@withContext RemoteFetchResult.Success(
                source = source,
                calendar = parsedResult.getOrThrow()
            )
        }
        RemoteFetchResult.Failure(lastError)
    }

    private fun buildStatus(isSyncing: Boolean = false): HolidayCalendarSyncStatus {
        val calendar = runCatching { holidayCalendarStore.currentCalendar() }
            .getOrElse { HolidayCalendar.empty() }
        val expired = calendar.isExpired(todayProvider())
        val warning = if (expired && lastSyncFailed) {
            true
        } else {
            false
        }
        return HolidayCalendarSyncStatus(
            coverageEnd = calendar.latestCoveredDate(),
            sourceName = calendar.sourceName,
            sourceUrl = calendar.sourceUrl,
            syncedAt = calendar.syncedAt,
            isExpired = expired,
            isSyncing = isSyncing,
            lastErrorMessage = lastSyncErrorMessage,
            shouldWarnSourceUnavailable = warning
        )
    }

    private fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CustomAlarm/1.0")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                error("HTTP $code")
            }
            BufferedInputStream(connection.inputStream).bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private sealed interface RemoteFetchResult {
        data class Success(
            val source: HolidayCalendarRemoteSource,
            val calendar: HolidayCalendar
        ) : RemoteFetchResult

        data class Failure(val message: String) : RemoteFetchResult
    }
}
