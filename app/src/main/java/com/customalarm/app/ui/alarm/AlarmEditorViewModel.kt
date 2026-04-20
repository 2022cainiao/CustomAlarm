package com.customalarm.app.ui.alarm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.customalarm.app.AppContainer
import com.customalarm.app.data.model.AlarmType
import com.customalarm.app.data.model.RoutineGroupEntity
import com.customalarm.app.domain.AlarmCoordinator
import com.customalarm.app.domain.AlarmDraft
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AlarmEditorUiState(
    val id: Long = 0L,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val type: AlarmType = AlarmType.NORMAL,
    val routineGroupId: Long? = null,
    val hour: String = "07",
    val minute: String = "00",
    val repeatDays: Set<Int> = emptySet(),
    val label: String = "",
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: String = "10",
    val enabled: Boolean = true,
    val availableGroups: List<RoutineGroupEntity> = emptyList(),
    val errorMessage: String? = null,
    val saved: Boolean = false
)

class AlarmEditorViewModel(
    private val container: AppContainer,
    private val coordinator: AlarmCoordinator,
    private val alarmId: Long?,
    private val presetRoutineGroupId: Long?
) : ViewModel() {
    var uiState by mutableStateOf(AlarmEditorUiState())
        private set

    init {
        viewModelScope.launch {
            val groups = container.routineGroupRepository.observeRoutineGroupsWithAlarms().first().map { it.group }
            val defaultSnooze = container.appSettingsRepository.defaultSnoozeMinutes.first()
            uiState = uiState.copy(
                isLoading = false,
                availableGroups = groups,
                snoozeMinutes = defaultSnooze.toString(),
                type = if (presetRoutineGroupId != null) AlarmType.ROUTINE else AlarmType.NORMAL,
                routineGroupId = presetRoutineGroupId
            )

            if (alarmId != null) {
                val existing = container.alarmRepository.getAlarm(alarmId) ?: return@launch
                uiState = uiState.copy(
                    id = existing.id,
                    type = existing.type,
                    routineGroupId = existing.routineGroupId,
                    hour = existing.hour.toString().padStart(2, '0'),
                    minute = existing.minute.toString().padStart(2, '0'),
                    repeatDays = existing.repeatDays.toSet(),
                    label = existing.label,
                    ringtoneUri = existing.ringtoneUri,
                    vibrate = existing.vibrate,
                    snoozeEnabled = existing.snoozeEnabled,
                    snoozeMinutes = existing.snoozeMinutes.toString(),
                    enabled = existing.enabled
                )
            }
        }
    }

    fun updateHour(value: String) {
        uiState = uiState.copy(hour = value.take(2), errorMessage = null)
    }

    fun updateMinute(value: String) {
        uiState = uiState.copy(minute = value.take(2), errorMessage = null)
    }

    fun updateLabel(value: String) {
        uiState = uiState.copy(label = value, errorMessage = null)
    }

    fun updateSnoozeMinutes(value: String) {
        uiState = uiState.copy(snoozeMinutes = value.take(2), errorMessage = null)
    }

    fun updateType(type: AlarmType) {
        uiState = uiState.copy(
            type = type,
            routineGroupId = if (type == AlarmType.ROUTINE) uiState.routineGroupId ?: uiState.availableGroups.firstOrNull()?.id else null,
            errorMessage = null
        )
    }

    fun updateRoutineGroupId(groupId: Long?) {
        uiState = uiState.copy(routineGroupId = groupId, errorMessage = null)
    }

    fun toggleRepeatDay(day: Int) {
        val updated = uiState.repeatDays.toMutableSet().apply {
            if (contains(day)) remove(day) else add(day)
        }
        uiState = uiState.copy(repeatDays = updated, errorMessage = null)
    }

    fun updateVibrate(enabled: Boolean) {
        uiState = uiState.copy(vibrate = enabled)
    }

    fun updateSnoozeEnabled(enabled: Boolean) {
        uiState = uiState.copy(snoozeEnabled = enabled)
    }

    fun updateEnabled(enabled: Boolean) {
        uiState = uiState.copy(enabled = enabled)
    }

    fun updateRingtone(uri: String?) {
        uiState = uiState.copy(ringtoneUri = uri)
    }

    fun save() {
        val hour = uiState.hour.toIntOrNull()
        val minute = uiState.minute.toIntOrNull()
        val snooze = uiState.snoozeMinutes.toIntOrNull()
        if (hour == null || hour !in 0..23 || minute == null || minute !in 0..59) {
            uiState = uiState.copy(errorMessage = "请输入正确的时间")
            return
        }
        if (uiState.type == AlarmType.ROUTINE && uiState.routineGroupId == null) {
            uiState = uiState.copy(errorMessage = "请选择作息组")
            return
        }
        if (uiState.snoozeEnabled && (snooze == null || snooze !in 1..30)) {
            uiState = uiState.copy(errorMessage = "贪睡时长请设置在 1-30 分钟")
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true, errorMessage = null)
            coordinator.saveAlarm(
                AlarmDraft(
                    id = uiState.id,
                    type = uiState.type,
                    routineGroupId = if (uiState.type == AlarmType.ROUTINE) uiState.routineGroupId else null,
                    hour = hour,
                    minute = minute,
                    repeatDays = uiState.repeatDays.sorted(),
                    label = uiState.label,
                    ringtoneUri = uiState.ringtoneUri,
                    vibrate = uiState.vibrate,
                    snoozeEnabled = uiState.snoozeEnabled,
                    snoozeMinutes = snooze ?: 10,
                    enabled = uiState.enabled
                )
            )
            if (uiState.snoozeEnabled && snooze != null) {
                container.appSettingsRepository.setDefaultSnoozeMinutes(snooze)
            }
            uiState = uiState.copy(isSaving = false, saved = true)
        }
    }

    companion object {
        fun factory(
            container: AppContainer,
            alarmId: Long?,
            presetRoutineGroupId: Long?
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return AlarmEditorViewModel(
                        container = container,
                        coordinator = container.alarmCoordinator,
                        alarmId = alarmId,
                        presetRoutineGroupId = presetRoutineGroupId
                    ) as T
                }
            }
        }
    }
}

