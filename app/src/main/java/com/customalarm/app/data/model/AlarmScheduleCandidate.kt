package com.customalarm.app.data.model

import androidx.room.Embedded

data class AlarmScheduleCandidate(
    @Embedded
    val alarm: AlarmEntity,
    val routineGroupEnabled: Boolean?
) {
    val isEffectivelyEnabled: Boolean
        get() = when (alarm.type) {
            AlarmType.NORMAL -> alarm.enabled
            AlarmType.ROUTINE -> alarm.enabled && routineGroupEnabled == true
        }
}

