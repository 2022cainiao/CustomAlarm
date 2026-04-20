package com.customalarm.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.customalarm.app.AppContainer
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.model.RoutineGroupWithAlarms
import com.customalarm.app.domain.AlarmCoordinator
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
    val activeCount: Int,
    val nextTriggerAt: Long?
)

data class HomeUiState(
    val normalAlarms: List<AlarmEntity> = emptyList(),
    val routineGroups: List<RoutineGroupSummary> = emptyList()
)

class HomeViewModel(
    private val coordinator: AlarmCoordinator,
    private val homeStateFlow: StateFlow<HomeUiState>
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = homeStateFlow

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

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory {
            val state = combine(
                container.alarmRepository.observeNormalAlarms(),
                container.routineGroupRepository.observeRoutineGroupsWithAlarms()
            ) { normalAlarms, routineGroups ->
                HomeUiState(
                    normalAlarms = normalAlarms,
                    routineGroups = routineGroups.map { it.toSummary() }
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
        activeCount = effectiveAlarms.size,
        nextTriggerAt = effectiveAlarms.mapNotNull { it.nextTriggerAt }.minOrNull()
    )
}
