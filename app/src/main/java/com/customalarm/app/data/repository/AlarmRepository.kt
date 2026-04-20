package com.customalarm.app.data.repository

import com.customalarm.app.data.db.AlarmDao
import com.customalarm.app.data.model.AlarmEntity
import com.customalarm.app.data.model.AlarmScheduleCandidate
import com.customalarm.app.domain.NextTriggerCalculator
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val nextTriggerCalculator: NextTriggerCalculator
) {
    fun observeNormalAlarms(): Flow<List<AlarmEntity>> = alarmDao.observeNormalAlarms()

    fun observeRoutineGroupAlarms(groupId: Long): Flow<List<AlarmEntity>> =
        alarmDao.observeRoutineGroupAlarms(groupId)

    suspend fun getAlarm(alarmId: Long): AlarmEntity? = alarmDao.getAlarmById(alarmId)

    suspend fun getRoutineGroupAlarms(groupId: Long): List<AlarmEntity> = alarmDao.getRoutineGroupAlarms(groupId)

    suspend fun getScheduleCandidates(): List<AlarmScheduleCandidate> = alarmDao.getScheduleCandidates()

    suspend fun saveAlarm(alarm: AlarmEntity): Long {
        val prepared = alarm.withComputedNextTrigger()
        return if (prepared.id == 0L) {
            alarmDao.insert(prepared)
        } else {
            alarmDao.update(prepared)
            prepared.id
        }
    }

    suspend fun deleteAlarm(alarmId: Long) {
        alarmDao.deleteById(alarmId)
    }

    suspend fun deleteAlarmsByRoutineGroup(groupId: Long) {
        alarmDao.deleteByRoutineGroupId(groupId)
    }

    suspend fun setAlarmEnabled(alarmId: Long, enabled: Boolean) {
        val current = alarmDao.getAlarmById(alarmId) ?: return
        saveAlarm(current.copy(enabled = enabled))
    }

    suspend fun onAlarmTriggered(alarmId: Long) {
        val current = alarmDao.getAlarmById(alarmId) ?: return
        val updated = if (current.repeatDays.isEmpty()) {
            current.copy(enabled = false, nextTriggerAt = null)
        } else {
            current.copy(
                nextTriggerAt = nextTriggerCalculator.calculateNextTrigger(
                    hour = current.hour,
                    minute = current.minute,
                    repeatDays = current.repeatDays
                )
            )
        }
        alarmDao.update(updated)
    }

    suspend fun snoozeAlarm(alarmId: Long, snoozeMinutes: Int) {
        val current = alarmDao.getAlarmById(alarmId) ?: return
        val nextTrigger = System.currentTimeMillis() + (snoozeMinutes * 60_000L)
        alarmDao.update(current.copy(enabled = true, nextTriggerAt = nextTrigger))
    }

    private fun AlarmEntity.withComputedNextTrigger(): AlarmEntity {
        if (!enabled) return copy(nextTriggerAt = null)
        return copy(
            nextTriggerAt = nextTriggerCalculator.calculateNextTrigger(
                hour = hour,
                minute = minute,
                repeatDays = repeatDays
            )
        )
    }
}

