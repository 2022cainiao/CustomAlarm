package com.customalarm.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "alarms",
    foreignKeys = [
        ForeignKey(
            entity = RoutineGroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["routineGroupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("routineGroupId"), Index("enabled")]
)
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: AlarmType = AlarmType.NORMAL,
    val routineGroupId: Long? = null,
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: List<Int> = emptyList(),
    val label: String = "",
    val ringtoneUri: String? = null,
    val vibrate: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val enabled: Boolean = true,
    val nextTriggerAt: Long? = null
)

