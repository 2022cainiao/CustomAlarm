package com.customalarm.app.ui.routine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.customalarm.app.AppContainer
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.domain.AlarmCoordinator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class RoutineMoveTarget(
    val groupId: Long,
    val groupName: String
)

data class RoutineDetailUiState(
    val groupId: Long = 0L,
    val groupName: String = "",
    val enabled: Boolean = true,
    val alarms: List<AlarmEntity> = emptyList(),
    val moveTargets: List<RoutineMoveTarget> = emptyList(),
    val exists: Boolean = true
)

class RoutineDetailViewModel(
    private val coordinator: AlarmCoordinator,
    private val stateFlow: StateFlow<RoutineDetailUiState>
) : ViewModel() {
    val uiState: StateFlow<RoutineDetailUiState> = stateFlow

    fun toggleGroup(enabled: Boolean) {
        viewModelScope.launch {
            coordinator.toggleRoutineGroup(uiState.value.groupId, enabled)
        }
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch {
            coordinator.toggleAlarm(alarmId, enabled)
        }
    }

    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            coordinator.deleteAlarm(alarmId)
        }
    }

    fun moveAlarmToStandard(alarmId: Long) {
        viewModelScope.launch {
            coordinator.moveAlarmToStandard(alarmId)
        }
    }

    fun moveAlarmToGroup(alarmId: Long, groupId: Long) {
        viewModelScope.launch {
            coordinator.moveAlarmToRoutineGroup(alarmId, groupId)
        }
    }

    fun deleteGroup(onDeleted: () -> Unit) {
        viewModelScope.launch {
            coordinator.deleteRoutineGroup(uiState.value.groupId)
            onDeleted()
        }
    }

    companion object {
        fun factory(container: AppContainer, groupId: Long): ViewModelProvider.Factory {
            val state = combine(
                container.routineGroupRepository.observeRoutineGroup(groupId),
                container.routineGroupRepository.observeRoutineGroupsWithAlarms()
            ) { relation, allGroups ->
                    if (relation == null) {
                        RoutineDetailUiState(exists = false)
                    } else {
                        RoutineDetailUiState(
                            groupId = relation.group.id,
                            groupName = relation.group.name,
                            enabled = relation.group.enabled,
                            alarms = relation.alarms.sortedWith(compareBy({ it.hour }, { it.minute }, { it.id })),
                            moveTargets = allGroups
                                .map { it.group }
                                .filter { it.id != relation.group.id }
                                .sortedBy { it.sortOrder }
                                .map { RoutineMoveTarget(groupId = it.id, groupName = it.name) },
                            exists = true
                        )
                    }
                }
                .stateIn(
                    scope = container.applicationScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = RoutineDetailUiState(groupId = groupId)
                )
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RoutineDetailViewModel(container.alarmCoordinator, state) as T
                }
            }
        }
    }
}
