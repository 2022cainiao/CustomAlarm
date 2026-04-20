package com.customalarm.app.data.model

import androidx.room.Embedded
import androidx.room.Relation

data class RoutineGroupWithAlarms(
    @Embedded
    val group: RoutineGroupEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "routineGroupId"
    )
    val alarms: List<AlarmEntity>
)

