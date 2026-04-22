package com.customalarm.app.ui.alarm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.customalarm.app.R
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
    val holidayAwareWorkdays: Boolean = false,
    val label: String = "",
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: String = "10",
    val enabled: Boolean = true,
    val availableGroups: List<RoutineGroupEntity> = emptyList(),
    val errorMessageRes: Int? = null,
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
                    holidayAwareWorkdays = existing.holidayAwareWorkdays,
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
        uiState = uiState.copy(hour = value.filter(Char::isDigit).take(2), errorMessageRes = null)
    }

    fun updateMinute(value: String) {
        uiState = uiState.copy(minute = value.filter(Char::isDigit).take(2), errorMessageRes = null)
    }

    fun updateLabel(value: String) {
        uiState = uiState.copy(label = value, errorMessageRes = null)
    }

    fun updateSnoozeMinutes(value: String) {
        uiState = uiState.copy(snoozeMinutes = value.filter(Char::isDigit).take(2), errorMessageRes = null)
    }

    fun updateType(type: AlarmType) {
        uiState = uiState.copy(
            type = type,
            routineGroupId = if (type == AlarmType.ROUTINE) {
                uiState.routineGroupId ?: uiState.availableGroups.firstOrNull()?.id
            } else {
                null
            },
            errorMessageRes = null
        )
    }

    fun updateRoutineGroupId(groupId: Long?) {
        uiState = uiState.copy(routineGroupId = groupId, errorMessageRes = null)
    }

    fun toggleRepeatDay(day: Int) {
        val updated = uiState.repeatDays.toMutableSet().apply {
            if (contains(day)) remove(day) else add(day)
        }
        uiState = uiState.copy(
            repeatDays = updated,
            holidayAwareWorkdays = uiState.holidayAwareWorkdays && updated == WORKDAY_SET,
            errorMessageRes = null
        )
    }

    fun updateHolidayAwareWorkdays(enabled: Boolean) {
        uiState = uiState.copy(
            repeatDays = if (enabled) WORKDAY_SET else uiState.repeatDays,
            holidayAwareWorkdays = enabled,
            errorMessageRes = null
        )
    }

    fun updateVibrate(enabled: Boolean) {
        uiState = uiState.copy(vibrate = enabled)
    }

    fun updateSnoozeEnabled(enabled: Boolean) {
        uiState = uiState.copy(snoozeEnabled = enabled, errorMessageRes = null)
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
            uiState = uiState.copy(errorMessageRes = R.string.error_invalid_time)
            return
        }
        if (uiState.type == AlarmType.ROUTINE && uiState.routineGroupId == null) {
            uiState = uiState.copy(errorMessageRes = R.string.error_choose_routine_group)
            return
        }
        if (uiState.snoozeEnabled && (snooze == null || snooze !in 1..30)) {
            uiState = uiState.copy(errorMessageRes = R.string.error_snooze_range)
            return
        }

        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true, errorMessageRes = null)
            coordinator.saveAlarm(
                AlarmDraft(
                    id = uiState.id,
                    type = uiState.type,
                    routineGroupId = if (uiState.type == AlarmType.ROUTINE) uiState.routineGroupId else null,
                    hour = hour,
                    minute = minute,
                    repeatDays = uiState.repeatDays.sorted(),
                    holidayAwareWorkdays = uiState.holidayAwareWorkdays,
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
        private val WORKDAY_SET = setOf(1, 2, 3, 4, 5)

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
