package com.customalarm.app.domain

import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.model.AlarmType
import com.customalarm.app.data.model.RoutineGroupEntity
import com.customalarm.app.data.repository.AlarmRepository
import com.customalarm.app.data.repository.RoutineGroupRepository

data class AlarmDraft(
    val id: Long = 0L,
    val type: AlarmType = AlarmType.NORMAL,
    val routineGroupId: Long? = null,
    val hour: Int,
    val minute: Int,
    val repeatDays: List<Int>,
    val label: String,
    val ringtoneUri: String?,
    val vibrate: Boolean,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val enabled: Boolean
)

data class RoutineGroupDraft(
    val id: Long = 0L,
    val name: String,
    val enabled: Boolean
)

class AlarmCoordinator(
    private val alarmRepository: AlarmRepository,
    private val routineGroupRepository: RoutineGroupRepository,
    private val alarmScheduler: AlarmScheduler
) {
    suspend fun saveAlarm(draft: AlarmDraft): Long {
        val alarmId = alarmRepository.saveAlarm(
            AlarmEntity(
                id = draft.id,
                type = draft.type,
                routineGroupId = draft.routineGroupId,
                hour = draft.hour,
                minute = draft.minute,
                repeatDays = draft.repeatDays.sorted(),
                label = draft.label.trim(),
                ringtoneUri = draft.ringtoneUri,
                vibrate = draft.vibrate,
                snoozeEnabled = draft.snoozeEnabled,
                snoozeMinutes = draft.snoozeMinutes,
                enabled = draft.enabled
            )
        )
        alarmScheduler.refreshAll()
        return alarmId
    }

    suspend fun toggleAlarm(alarmId: Long, enabled: Boolean) {
        alarmRepository.setAlarmEnabled(alarmId, enabled)
        alarmScheduler.refreshAll()
    }

    suspend fun deleteAlarm(alarmId: Long) {
        alarmScheduler.cancelAlarm(alarmId)
        alarmRepository.deleteAlarm(alarmId)
        alarmScheduler.refreshAll()
    }

    suspend fun moveAlarmToStandard(alarmId: Long) {
        val current = alarmRepository.getAlarm(alarmId) ?: return
        alarmRepository.saveAlarm(
            current.copy(
                type = AlarmType.NORMAL,
                routineGroupId = null
            )
        )
        alarmScheduler.refreshAll()
    }

    suspend fun moveAlarmToRoutineGroup(alarmId: Long, routineGroupId: Long) {
        val current = alarmRepository.getAlarm(alarmId) ?: return
        alarmRepository.saveAlarm(
            current.copy(
                type = AlarmType.ROUTINE,
                routineGroupId = routineGroupId
            )
        )
        alarmScheduler.refreshAll()
    }

    suspend fun saveRoutineGroup(draft: RoutineGroupDraft): Long {
        val existing = if (draft.id == 0L) null else routineGroupRepository.getRoutineGroup(draft.id)
        val groupId = routineGroupRepository.saveRoutineGroup(
            RoutineGroupEntity(
                id = draft.id,
                name = draft.name.trim(),
                enabled = draft.enabled,
                sortOrder = existing?.sortOrder ?: 0,
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
        )
        alarmScheduler.refreshAll()
        return groupId
    }

    suspend fun toggleRoutineGroup(groupId: Long, enabled: Boolean) {
        routineGroupRepository.setRoutineGroupEnabled(groupId, enabled)
        alarmScheduler.refreshAll()
    }

    suspend fun deleteRoutineGroup(groupId: Long) {
        alarmRepository.getRoutineGroupAlarms(groupId).forEach { alarm ->
            alarmScheduler.cancelAlarm(alarm.id)
        }
        alarmRepository.deleteAlarmsByRoutineGroup(groupId)
        routineGroupRepository.deleteRoutineGroup(groupId)
        alarmScheduler.refreshAll()
    }

    suspend fun onAlarmTriggered(alarmId: Long) {
        alarmRepository.onAlarmTriggered(alarmId)
        alarmScheduler.refreshAll()
    }

    suspend fun snoozeAlarm(alarmId: Long, minutes: Int) {
        alarmRepository.snoozeAlarm(alarmId, minutes)
        alarmScheduler.refreshAll()
    }

    suspend fun rebuildSchedules() {
        alarmScheduler.refreshAll()
    }
}
