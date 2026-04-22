package com.customalarm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.customalarm.app.AppContainer
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.model.RoutineGroupWithAlarms
import com.customalarm.app.data.repository.AppLanguage
import com.customalarm.app.data.repository.AppSettingsRepository
import com.customalarm.app.domain.AlarmCoordinator
import com.customalarm.app.domain.HolidayCalendarSyncRepository
import com.customalarm.app.domain.HolidayCalendarSyncStatus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineGroupSummary(
    val id: Long,
    val name: String,
    val enabled: Boolean,
    val alarms: List<AlarmEntity>,
    val alarmCount: Int,
    val activeCount: Int,
    val nextTriggerAt: Long?
)

data class UpcomingAlarmSummary(
    val alarmId: Long,
    val routineGroupId: Long?,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: List<Int>,
    val holidayAwareWorkdays: Boolean,
    val nextTriggerAt: Long,
    val isRoutineAlarm: Boolean,
    val routineGroupName: String? = null
)

data class HolidayCalendarUiState(
    val coverageEnd: java.time.LocalDate? = null,
    val sourceName: String? = null,
    val sourceUrl: String? = null,
    val syncedAt: java.time.Instant? = null,
    val isExpired: Boolean = false,
    val isSyncing: Boolean = false,
    val lastErrorMessage: String? = null,
    val shouldWarnSourceUnavailable: Boolean = false
)

data class HomeUiState(
    val normalAlarms: List<AlarmEntity> = emptyList(),
    val routineGroups: List<RoutineGroupSummary> = emptyList(),
    val upcomingAlarms: List<UpcomingAlarmSummary> = emptyList(),
    val holidayCalendar: HolidayCalendarUiState = HolidayCalendarUiState(),
    val holidayServerUrl: String = "",
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val totalAlarmCount: Int = 0,
    val enabledNormalCount: Int = 0,
    val routineGroupCount: Int = 0,
    val enabledRoutineGroupCount: Int = 0,
    val effectiveRoutineAlarmCount: Int = 0,
    val nextTriggerAt: Long? = null
)

class HomeViewModel(
    private val coordinator: AlarmCoordinator,
    private val appSettingsRepository: AppSettingsRepository,
    private val holidayCalendarSyncRepository: HolidayCalendarSyncRepository,
    private val homeStateFlow: StateFlow<HomeUiState>
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = homeStateFlow

    init {
        viewModelScope.launch {
            runCatching {
                val result = holidayCalendarSyncRepository.syncIfNeeded()
                if (result.succeeded && result.calendarChanged) {
                    coordinator.rebuildSchedules()
                }
            }
        }
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            coordinator.toggleAlarm(alarmId, enabled)
        }
    }

    fun toggleRoutineGroup(groupId: Long, enabled: Boolean) {
        viewModelScope.launch {
            coordinator.toggleRoutineGroup(groupId, enabled)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            coordinator.deleteAlarm(alarmId)
        }
    }

    fun moveAlarmToRoutineGroup(alarmId: Long, groupId: Long) {
        viewModelScope.launch {
            coordinator.moveAlarmToRoutineGroup(alarmId, groupId)
        }
    }

    fun syncHolidayCalendar(serverUrl: String) {
        viewModelScope.launch {
            appSettingsRepository.setHolidaySyncUrl(serverUrl)
            holidayCalendarSyncRepository.refreshStatus()
            runCatching {
                val result = holidayCalendarSyncRepository.syncNow()
                if (result.succeeded && result.calendarChanged) {
                    coordinator.rebuildSchedules()
                }
            }
        }
    }

    fun saveHolidayServerUrl(url: String) {
        viewModelScope.launch {
            appSettingsRepository.setHolidaySyncUrl(url)
            holidayCalendarSyncRepository.refreshStatus()
        }
    }

    fun setAppLanguage(language: AppLanguage) {
        viewModelScope.launch {
            appSettingsRepository.setAppLanguage(language)
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            val state = combine(
                container.alarmRepository.observeNormalAlarms(),
                container.routineGroupRepository.observeRoutineGroupsWithAlarms(),
                container.holidayCalendarSyncRepository.status,
                container.appSettingsRepository.appLanguage,
                container.appSettingsRepository.holidaySyncUrl
            ) { normalAlarms, routineGroups, holidayStatus, appLanguage, holidayServerUrl ->
                val sortedNormalAlarms = normalAlarms.sortedWith(
                    compareBy<AlarmEntity> { it.nextTriggerAt ?: Long.MAX_VALUE }
                        .thenBy { it.id }
                )
                val routineSummaries = routineGroups
                    .map { it.toSummary() }
                    .sortedWith(
                        compareBy<RoutineGroupSummary> { it.nextTriggerAt ?: Long.MAX_VALUE }
                            .thenBy { it.name.lowercase() }
                    )
                val effectiveNormalAlarms = sortedNormalAlarms.filter {
                    it.enabled && it.nextTriggerAt != null
                }
                val effectiveRoutineAlarms = routineGroups.flatMap { groupWithAlarms ->
                    groupWithAlarms.alarms
                        .filter { it.enabled && groupWithAlarms.group.enabled && it.nextTriggerAt != null }
                        .map { alarm ->
                            UpcomingAlarmSummary(
                                alarmId = alarm.id,
                                routineGroupId = groupWithAlarms.group.id,
                                hour = alarm.hour,
                                minute = alarm.minute,
                                label = alarm.label,
                                repeatDays = alarm.repeatDays,
                                holidayAwareWorkdays = alarm.holidayAwareWorkdays,
                                nextTriggerAt = requireNotNull(alarm.nextTriggerAt),
                                isRoutineAlarm = true,
                                routineGroupName = groupWithAlarms.group.name
                            )
                        }
                }
                val upcomingAlarms = (
                    effectiveNormalAlarms.map { alarm ->
                        UpcomingAlarmSummary(
                            alarmId = alarm.id,
                            routineGroupId = null,
                            hour = alarm.hour,
                            minute = alarm.minute,
                            label = alarm.label,
                            repeatDays = alarm.repeatDays,
                            holidayAwareWorkdays = alarm.holidayAwareWorkdays,
                            nextTriggerAt = requireNotNull(alarm.nextTriggerAt),
                            isRoutineAlarm = false
                        )
                    } + effectiveRoutineAlarms
                    )
                    .sortedBy { it.nextTriggerAt }
                    .take(6)

                HomeUiState(
                    normalAlarms = sortedNormalAlarms,
                    routineGroups = routineSummaries,
                    upcomingAlarms = upcomingAlarms,
                    holidayCalendar = holidayStatus.toUiState(),
                    holidayServerUrl = holidayServerUrl,
                    appLanguage = appLanguage,
                    totalAlarmCount = sortedNormalAlarms.size + routineGroups.sumOf { it.alarms.size },
                    enabledNormalCount = sortedNormalAlarms.count { it.enabled },
                    routineGroupCount = routineSummaries.size,
                    enabledRoutineGroupCount = routineSummaries.count { it.enabled },
                    effectiveRoutineAlarmCount = routineSummaries.sumOf { it.activeCount },
                    nextTriggerAt = upcomingAlarms.firstOrNull()?.nextTriggerAt
                )
            }.stateIn(
                scope = container.applicationScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = HomeUiState()
            )
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(
                        coordinator = container.alarmCoordinator,
                        appSettingsRepository = container.appSettingsRepository,
                        holidayCalendarSyncRepository = container.holidayCalendarSyncRepository,
                        homeStateFlow = state
                    ) as T
                }
            }
        }
    }
}

private fun RoutineGroupWithAlarms.toSummary(): RoutineGroupSummary {
    val effectiveAlarms = alarms.filter { it.enabled && group.enabled }
    return RoutineGroupSummary(
        id = group.id,
        name = group.name,
        enabled = group.enabled,
        alarms = alarms,
        alarmCount = alarms.size,
        activeCount = effectiveAlarms.size,
        nextTriggerAt = effectiveAlarms.mapNotNull { it.nextTriggerAt }.minOrNull()
    )
}

private fun HolidayCalendarSyncStatus.toUiState(): HolidayCalendarUiState {
    return HolidayCalendarUiState(
        coverageEnd = coverageEnd,
        sourceName = sourceName,
        sourceUrl = sourceUrl,
        syncedAt = syncedAt,
        isExpired = isExpired,
        isSyncing = isSyncing,
        lastErrorMessage = lastErrorMessage,
        shouldWarnSourceUnavailable = shouldWarnSourceUnavailable
    )
}
